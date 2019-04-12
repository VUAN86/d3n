package de.ascendro.f4m.service.voucher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.*;

import javax.inject.Inject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.analytics.model.VoucherCountEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.voucher.dao.VoucherAerospikeDao;
import de.ascendro.f4m.service.voucher.model.UserVoucher;
import de.ascendro.f4m.service.voucher.model.UserVoucherReferenceEntry;
import de.ascendro.f4m.service.voucher.model.UserVoucherResponseModel;
import de.ascendro.f4m.service.voucher.model.UserVoucherType;
import de.ascendro.f4m.service.voucher.model.Voucher;
import de.ascendro.f4m.service.voucher.model.VoucherResponseModel;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByUserRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByUserResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByVoucherRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListByVoucherResponse;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListRequest;
import de.ascendro.f4m.service.voucher.model.uservoucher.UserVoucherListResponse;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherListRequest;
import de.ascendro.f4m.service.voucher.model.voucher.VoucherListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities to process Voucher Voucher data.
 */
public class VoucherUtil {

	private static final String USER_VOUCHER_ID_ITEMS_SEPARATOR = ":";

	private final VoucherAerospikeDao voucherAerospikeDao;
	private final CommonProfileAerospikeDao commonProfileAerospikeDao;
	private final JsonUtil jsonUtil;
	private final Tracker tracker;
	private static final Logger LOGGER = LoggerFactory.getLogger(VoucherUtil.class);

	/**
	 * Utilities to process Voucher Voucher data.
	 */
	@Inject
	public VoucherUtil(VoucherAerospikeDao voucherAerospikeDao, CommonProfileAerospikeDao commonProfileAerospikeDao,
			JsonUtil jsonUtil, Tracker tracker) {
		this.voucherAerospikeDao = voucherAerospikeDao;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
		this.jsonUtil = jsonUtil;
		this.tracker = tracker;
	}

	public UserVoucherListByVoucherResponse getUserVouchersByVoucherId(UserVoucherListByVoucherRequest request) {
		UserVoucherListByVoucherResponse response;

		Voucher voucher = voucherAerospikeDao.getVoucherById(request.getVoucherId());

		int limit = request.getLimit();
		long offset = request.getOffset();
		String voucherId = request.getVoucherId();
		UserVoucherType userVoucherType = Objects.nonNull(request.getType())
				? UserVoucherType.getByText(request.getType())
				: null;
		long totalNumberOfUserVouchers = voucherAerospikeDao.getTotalUserVouchersCountByType(voucherId,
				userVoucherType);

		if (limit > UserVoucherListByVoucherRequest.MAX_LIST_LIMIT) {
			throw new F4MValidationFailedException(String.format("List limit exceeded (max: %s, specified: %s)",
					UserVoucherListByVoucherRequest.MAX_LIST_LIMIT, limit));
		}
		if (limit < 0) {
			throw new F4MValidationFailedException("List must have positive limit");
		}

		if (voucher == null || totalNumberOfUserVouchers == 0) {
			response = new UserVoucherListByVoucherResponse(limit, offset);
		} else {
			if (offset + limit > totalNumberOfUserVouchers) {
				limit = (int) (totalNumberOfUserVouchers - offset);
			}

			List<UserVoucher> userVoucherList = voucherAerospikeDao.getUserVoucherListByVoucherId(limit, offset,
					voucherId, userVoucherType);

			List<JsonObject> extendedUserVoucherList = mergeUserVoucherListWithVoucher(userVoucherList, voucher);

			response = new UserVoucherListByVoucherResponse(request.getLimit(), request.getOffset(),
					totalNumberOfUserVouchers, extendedUserVoucherList);
		}

		return response;
	}

	public UserVoucherListByUserResponse getUserVouchersByUserId(UserVoucherListByUserRequest request) {

		List<JsonObject> extendedUserVoucherList = new ArrayList<>();

		JsonArray voucherReferences = commonProfileAerospikeDao.getVoucherReferencesArrayFromBlob(request.getUserId());

		long offset = request.getOffset();
		int limit = request.getLimit();
		long total = 0;
		offset = (offset < voucherReferences.size()) ? offset : voucherReferences.size();
		long finalLimit = (offset + limit < voucherReferences.size()) ? offset + limit : voucherReferences.size();
		for (long i = offset; i < finalLimit; i++) {
			JsonElement voucherReferenceJson = voucherReferences.get((int) i);

			UserVoucherReferenceEntry voucherReference = jsonUtil.fromJson(voucherReferenceJson,
					UserVoucherReferenceEntry.class);
			String voucherId = voucherReference.getVoucherId();
			String userVoucherId = voucherReference.getUserVoucherId();

			Voucher voucher = voucherAerospikeDao.getVoucherById(voucherId);
			UserVoucher userVoucher = voucherAerospikeDao.getUserVoucher(voucherId, userVoucherId);

			String userVoucherIdForResponse = createUserVoucherIdForResponse(voucherId, userVoucherId);
			UserVoucherResponseModel userVoucherResponseModel = new UserVoucherResponseModel(userVoucherIdForResponse,
					voucher, userVoucher);
			extendedUserVoucherList.add(jsonUtil.toJsonElement(userVoucherResponseModel).getAsJsonObject());
		}
		return new UserVoucherListByUserResponse((int) finalLimit, offset, total, extendedUserVoucherList);
	}

	public VoucherListResponse getVouchersByTenantId(VoucherListRequest request, String tenantId) {

		List<Voucher> vouchers = voucherAerospikeDao.getVouchersByTenantId(request.getLimit(), request.getOffset(),
				tenantId);

		// remove expired vouchers and those that don't have bonuspointsCosts set
		vouchers.removeIf(voucher -> voucher.getBonuspointsCosts() == null || DateTimeUtil
				.parseISODateTimeString(voucher.getExpirationDate()).isBefore(DateTimeUtil.getCurrentDateTime()));

		for (Voucher item : vouchers) {
			LOGGER.debug(item.getId());
			LOGGER.debug(item.getTitle());
		}
		LOGGER.debug(request.getSearchBy("regionalSettings"));
		if ( Objects.nonNull(request.getSearchBy("regionalSettings"))) {
			String searchTerms = request.getSearchBy("regionalSettings");
			Pattern MY_PATTERN = Pattern.compile(".*" + searchTerms.toLowerCase() + ".*");
			Pattern voucherSearch = Pattern.compile(".*" + searchTerms.toLowerCase() + ".*");

			vouchers.removeIf(voucher -> 
				!(Objects.nonNull(voucher.getRegionalSettings()) &&
				voucherSearch.matcher(voucher.getRegionalSettings().toLowerCase()).find())
			);
		} else {
			LOGGER.debug("Without serach");
		}
		for (Voucher item : vouchers) {
			LOGGER.debug(item.getId());
			LOGGER.debug(item.getTitle());
		}

		List<JsonObject> items = new ArrayList<>();

		for (Voucher item : vouchers) {
			VoucherResponseModel voucherModel = new VoucherResponseModel(item);
			JsonObject itemJsonObject = jsonUtil.toJsonElement(voucherModel).getAsJsonObject();
			items.add(itemJsonObject);
		}

		VoucherListResponse response = new VoucherListResponse(request.getLimit(), request.getOffset(), vouchers.size(),
				items);
		return response;
	}

	public UserVoucherListResponse getUnusedUserVouchersByUserId(String userId, UserVoucherListRequest request) {
		List<JsonObject> extendedUserVoucherList = new ArrayList<>();

		JsonArray voucherReferences = commonProfileAerospikeDao.getVoucherReferencesArrayFromBlob(userId);

		long offset = request.getOffset();
		int limit = request.getLimit();
		long total = 0;
		for (JsonElement voucherReferenceJson : voucherReferences) {
			UserVoucherReferenceEntry voucherReference = jsonUtil.fromJson(voucherReferenceJson,
					UserVoucherReferenceEntry.class);
			String voucherId = voucherReference.getVoucherId();
			String userVoucherId = voucherReference.getUserVoucherId();

			Voucher voucher = voucherAerospikeDao.getVoucherById(voucherId);
			UserVoucher userVoucher = voucherAerospikeDao.getUserVoucher(voucherId, userVoucherId);

			if (!userVoucher.isUsed()) {
				// Check if we add this item based on pagination
				if (total >= offset && total < offset + limit) {
					String userVoucherIdForResponse = createUserVoucherIdForResponse(voucherId, userVoucherId);
					UserVoucherResponseModel userVoucherResponseModel = new UserVoucherResponseModel(
							userVoucherIdForResponse, voucher, userVoucher);
					extendedUserVoucherList.add(jsonUtil.toJsonElement(userVoucherResponseModel).getAsJsonObject());
				}
				total++;
			}
		}
		if (offset + limit > total) {
			limit = (int) (total - offset);
		}

		return new UserVoucherListResponse(limit, offset, total, extendedUserVoucherList);
	}

	private List<JsonObject> mergeUserVoucherListWithVoucher(List<UserVoucher> userVoucherList, Voucher voucher) {
		List<JsonObject> extendedUserVoucherList = new ArrayList<>();

		for (UserVoucher userVoucher : userVoucherList) {
			String userVoucherIdForResponse = createUserVoucherIdForResponse(voucher.getId(), userVoucher.getId());
			UserVoucherResponseModel userVoucherResponseModel = new UserVoucherResponseModel(userVoucherIdForResponse,
					voucher, userVoucher);
			extendedUserVoucherList.add(jsonUtil.toJsonElement(userVoucherResponseModel).getAsJsonObject());
		}
		return extendedUserVoucherList;
	}

	public static String createUserVoucherIdForResponse(String voucherId, String userVoucherId) {
		return voucherId + USER_VOUCHER_ID_ITEMS_SEPARATOR + userVoucherId;
	}

	public String assignVoucherToUser(String voucherId, String userId, String gameInstanceId, String tombolaId, String reason) {
		// Assign voucher to profile
		String userVoucherId = voucherAerospikeDao.assignVoucherToUser(voucherId, userId, gameInstanceId, tombolaId, reason);
		commonProfileAerospikeDao.vouchersEntryAdd(userId, voucherId, userVoucherId);
		trackCounterEvent(voucherId);
		return userVoucherId;
	}

	public void useVoucherForUser(String userVoucherId, String userId) {
		voucherAerospikeDao.useVoucherForUser(userVoucherId, userId);
	}

	public void reserveVoucher(String voucherId) {
		voucherAerospikeDao.reserveVoucher(voucherId);
		trackCounterEvent(voucherId);
	}

	private void trackCounterEvent(String voucherId) {
		long voucherCount = voucherAerospikeDao.getTotalUserVouchersCountByType(voucherId, UserVoucherType.AVAILABLE);

		VoucherCountEvent voucherCountEvent = new VoucherCountEvent();
		voucherCountEvent.setVoucherId(Long.parseLong(voucherId));
		voucherCountEvent.setVoucherCount(voucherCount);

		tracker.addAnonymousEvent(voucherCountEvent);
	}

	public void releaseVoucher(String voucherId) {
		voucherAerospikeDao.releaseVoucher(voucherId);
		trackCounterEvent(voucherId);
	}

	public VoucherResponseModel getVoucherResponseObjectById(String voucherId) {
		Voucher voucher = voucherAerospikeDao.getVoucherById(voucherId);

		return voucher == null ? null : new VoucherResponseModel(voucher);
	}

	public Voucher getVoucherById(String voucherId) {
		return voucherAerospikeDao.getVoucherById(voucherId);
	}

	public UserVoucher getUserVoucherById(String userVoucherId) {
		UserVoucher userVoucher = voucherAerospikeDao.getUserVoucherById(userVoucherId);
		if (userVoucher == null) {
			throw new F4MEntryNotFoundException("User voucher with ID [" + userVoucherId + "] not found");
		}
		return userVoucher;
	}

	public UserVoucherResponseModel getUserVoucherResponseModelJsonById(String userVoucherId) {
		UserVoucher userVoucher = voucherAerospikeDao.getUserVoucherById(userVoucherId);
		if (userVoucher == null) {
			throw new F4MEntryNotFoundException("User voucher with ID [" + userVoucherId + "] not found");
		}
		Voucher voucher = voucherAerospikeDao.getVoucherByUserVoucherId(userVoucherId);
		if (voucher == null) {
			throw new F4MEntryNotFoundException("Voucher for user voucher with ID [" + userVoucherId + "] not found");
		}

		return new UserVoucherResponseModel(userVoucherId, voucher, userVoucher);
	}

	public void moveVouchers(String sourceUserId, String targetUserId) {
		JsonArray voucherReferences = commonProfileAerospikeDao.getVoucherReferencesArrayFromBlob(sourceUserId);

		// Update source user vouchers to have target user ID and add the references to target user
		for (JsonElement voucherReferenceJson : voucherReferences) {
			UserVoucherReferenceEntry voucherReference = jsonUtil.fromJson(voucherReferenceJson,
					UserVoucherReferenceEntry.class);
			String voucherId = voucherReference.getVoucherId();
			String userVoucherId = voucherReference.getUserVoucherId();
			voucherAerospikeDao.updateVoucherUser(voucherId, userVoucherId, targetUserId);
			commonProfileAerospikeDao.vouchersEntryAdd(targetUserId, voucherId, userVoucherId);
		}

		// Delete source user voucher references
		commonProfileAerospikeDao.deleteSubBlob(sourceUserId, CommonProfileAerospikeDao.USER_VOUCHER_BLOB_NAME);
	}

	public void deleteExpiredVoucherListEntries(String tenantId) {
		voucherAerospikeDao.removeExpiredVouchersFromVoucherList(tenantId);
	}

	public Long getVoucherIdByUserVoucherId(String userVoucherId) {
		return Long.parseLong(userVoucherId.split(":")[0]);
	}

}
