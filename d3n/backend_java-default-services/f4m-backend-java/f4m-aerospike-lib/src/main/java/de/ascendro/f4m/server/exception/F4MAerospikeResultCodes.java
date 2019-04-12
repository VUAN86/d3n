package de.ascendro.f4m.server.exception;

public enum F4MAerospikeResultCodes {
	AS_PROTO_RESULT_OK(0),
	AS_PROTO_RESULT_FAIL_UNKNOWN(1),
	AS_PROTO_RESULT_FAIL_NOTFOUND(2),
	AS_PROTO_RESULT_FAIL_GENERATION(3),
	AS_PROTO_RESULT_FAIL_PARAMETER(4),
	AS_PROTO_RESULT_FAIL_RECORD_EXISTS(5),
	AS_PROTO_RESULT_FAIL_BIN_EXISTS(6),
	AS_PROTO_RESULT_FAIL_CLUSTER_KEY_MISMATCH(7),
	AS_PROTO_RESULT_FAIL_PARTITION_OUT_OF_SPACE(8),
	AS_PROTO_RESULT_FAIL_TIMEOUT(9),
	AS_PROTO_RESULT_FAIL_NO_XDR(10),
	AS_PROTO_RESULT_FAIL_UNAVAILABLE(11),
	AS_PROTO_RESULT_FAIL_INCOMPATIBLE_TYPE(12),
	AS_PROTO_RESULT_FAIL_RECORD_TOO_BIG(13),
	AS_PROTO_RESULT_FAIL_KEY_BUSY(14),
	AS_PROTO_RESULT_FAIL_SCAN_ABORT(15),
	AS_PROTO_RESULT_FAIL_UNSUPPORTED_FEATURE(16),
	AS_PROTO_RESULT_FAIL_BIN_NOT_FOUND(17),
	AS_PROTO_RESULT_FAIL_DEVICE_OVERLOAD(18),
	AS_PROTO_RESULT_FAIL_KEY_MISMATCH(19),
	AS_PROTO_RESULT_FAIL_NAMESPACE(20),
	AS_PROTO_RESULT_FAIL_BIN_NAME(21),
	AS_PROTO_RESULT_FAIL_FORBIDDEN(22),
	AS_SEC_ERR_OK_LAST(50),
	AS_SEC_ERR_NOT_SUPPORTED(51),
	AS_SEC_ERR_NOT_ENABLED(52),
	AS_SEC_ERR_SCHEME(53),
	AS_SEC_ERR_COMMAND(54),
	AS_SEC_ERR_FIELD(55),
	AS_SEC_ERR_STATE(56),
	AS_SEC_ERR_USER(60),
	AS_SEC_ERR_USER_EXISTS(61),
	AS_SEC_ERR_PASSWORD(62),
	AS_SEC_ERR_EXPIRED_PASSWORD(63),
	AS_SEC_ERR_FORBIDDEN_PASSWORD(64),
	AS_SEC_ERR_CREDENTIAL(65),
	AS_SEC_ERR_ROLE(70),
	AS_SEC_ERR_ROLE_EXISTS(71),
	AS_SEC_ERR_PRIVILEGE(72),
	AS_SEC_ERR_AUTHENTICATED(80),
	AS_SEC_ERR_ROLE_VIOLATION(81),
	AS_PROTO_RESULT_FAIL_UDF_EXECUTION(100),
	AS_PROTO_RESULT_FAIL_COLLECTION_ITEM_NOT_FOUND(125),
	AS_PROTO_RESULT_FAIL_BATCH_DISABLED(150),
	AS_PROTO_RESULT_FAIL_BATCH_MAX_REQUESTS(151),
	AS_PROTO_RESULT_FAIL_BATCH_QUEUES_FULL(152),
	AS_PROTO_RESULT_FAIL_GEO_INVALID_GEOJSON(160),
	AS_PROTO_RESULT_FAIL_INDEX_FOUND(200),
	AS_PROTO_RESULT_FAIL_INDEX_NOT_FOUND(201),
	AS_PROTO_RESULT_FAIL_INDEX_OOM(202),
	AS_PROTO_RESULT_FAIL_INDEX_NOTREADABLE(203),
	AS_PROTO_RESULT_FAIL_INDEX_GENERIC(204),
	AS_PROTO_RESULT_FAIL_INDEX_NAME_MAXLEN(205),
	AS_PROTO_RESULT_FAIL_INDEX_MAXCOUNT(206),
	AS_PROTO_RESULT_FAIL_QUERY_USERABORT(210),
	AS_PROTO_RESULT_FAIL_QUERY_QUEUEFULL(211),
	AS_PROTO_RESULT_FAIL_QUERY_TIMEOUT(212),
	AS_PROTO_RESULT_FAIL_QUERY_CBERROR(213),
	AS_PROTO_RESULT_FAIL_QUERY_NETIO_ERR(214),
	AS_PROTO_RESULT_FAIL_QUERY_DUPLICATE(215),
	LDT_UNIQUE_KEY_ERROR(1402);
	
	private final int code;
	
	private F4MAerospikeResultCodes(int code){
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}

	public static F4MAerospikeResultCodes findByCode(int resultCode) {
		F4MAerospikeResultCodes code = null;
		for(F4MAerospikeResultCodes c : values()){
			if(c.getCode() == resultCode){
				code = c;
				break;
			}			
		}
		return code;
	}
}