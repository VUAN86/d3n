package de.ascendro.f4m.server.util;


import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.ObjectUtils;

import de.ascendro.f4m.service.json.model.ListResult;

public class TimeBasedPaginationUtil {

    private TimeBasedPaginationUtil() {
    }

    /**
     * Given that there exists a list of items that are kept separately divided by time unit (by day, by month, etc.),
     * this function will return a paginated view of those items within a time period,
     * sorted descending by time (newest first).
     *
     * @param offset - the offset for pagination. First returned item will be the one at the index of offset after the
     *               newest item before dateTimeTo
     * @param limit - the limit for pagination. The last returned item will be at the index of offset + limit
     * @param dateTimeFrom - the start date of the full time interval in which to search (which actually marks the end
     *                     of the interval because the items are sorted descending)
     * @param dateTimeTo - the end date of the full time interval in which to search (which actually marks the start of
     *                   the interval because the items are sorted descending)
     * @param getItemsForTimeUnit - given a datetime, this function must return the list of items in that time unit
     *                            division (e.g. the items in that hour/day/month etc.). The returned list does not
     *                            need to be sorted, that is handled here internally.
     * @param getAttributeToCompare - given one of the items in question, this function must return a datetime based on
     *                              the item or it's attributes that is to be used to compare and sort the items (e.g.
     *                              it can be the item's creation datetime, or any other similar datetime attribute)
     * @param getNumberOfItemsForTimeUnit - given a datetime, this function must return number of items in that time
     *                                    unit division (e.g. the number of items in that hour/day/month etc.)
     * @param timeUnitsAreEqual - given two datetimes, this function must determine wheter the time unit for the two
     *                          values is the same (e.g. if they have the same hour/day/month, etc.)
     * @param iterationCheck - given a datetime, this function should determine if that time unit is within the interval
     *                       that is iterated. The check should generally be based on dateTimeFrom (inclusively), which
     *                       is the end of the iteration interval due to the descending order.
     *                       Examples:
     *                       - for hour based intervals, return true the hour of the specified datetime is after or
     *                       equal to the hour of dateTimeFrom;
     *                       - for month based intervals, return true if the month of the specified datetime is after or
     *                       equal to the month of dateTimeFrom; etc.
     * @param iterationNextItem - given a datetime, this function should return the next time unit to be iterated, in
     *                          descending order: E.g.: for hour based intervals, return datetime minus an hour; for
     *                          month based intervals, return datetime minus a month; etc.
     * @param <T> - the type of the items within the list. The only requirement is that they contain (or are)
     *           a time unit field that is comparable
     * @return - the result of the pagination and sorting, which contains the page view list of items and pagination
     * attributes: limit, offset, total (total is the full number of items in the entire interval)
     */
    public static <T> ListResult<T> getPaginatedItems(long offset, int limit, ZonedDateTime dateTimeFrom,
            ZonedDateTime dateTimeTo, Function<ZonedDateTime, List<T>> getItemsForTimeUnit,
            Function<T, ZonedDateTime> getAttributeToCompare,
            Function<ZonedDateTime, Integer> getNumberOfItemsForTimeUnit,
            BiFunction<ZonedDateTime, ZonedDateTime, Boolean> timeUnitsAreEqual,
            Function<ZonedDateTime, Boolean> iterationCheck,
            Function<ZonedDateTime, ZonedDateTime> iterationNextItem) {

        List<T> itemsList = new ArrayList<>();
        int totalItems = 0;
        int remainingOffset = 0;
        int remainingTail = 0;
        int excludedItemsNumberFromStartDate = TimeBasedPaginationUtil.getNumberOfExcludedItems(
                getItemsForTimeUnit.apply(dateTimeFrom), itemDateTime -> itemDateTime.isBefore(dateTimeFrom),
                getAttributeToCompare);
        int excludedItemsNumberFromEndDate = TimeBasedPaginationUtil.getNumberOfExcludedItems(
                getItemsForTimeUnit.apply(dateTimeTo), itemDateTime -> itemDateTime.isAfter(dateTimeTo),
                getAttributeToCompare);
        for (ZonedDateTime date = dateTimeTo; iterationCheck.apply(date); date = iterationNextItem.apply(date)) {
            int nbOfItemsInTimeUnit = getNumberOfItemsForTimeUnit.apply(date);
            int nbOfItemsToIgnore = getNbOfItemsToIgnore(dateTimeFrom, dateTimeTo, timeUnitsAreEqual,
                    excludedItemsNumberFromStartDate, excludedItemsNumberFromEndDate, date);
            if (isTimeUnitWithItemsToAddToList(offset, limit, totalItems, nbOfItemsInTimeUnit, nbOfItemsToIgnore)) {
                itemsList.addAll(getItemsForTimeUnit.apply(date));
                // remember how many drawings in first day to ignore
                remainingOffset = TimeBasedPaginationUtil.updateRemainingOffset(offset, totalItems, remainingOffset,
                        excludedItemsNumberFromEndDate, timeUnitsAreEqual.apply(date, dateTimeTo));
                // remember how many drawings in last day to ignore
                remainingTail = TimeBasedPaginationUtil.updateRemainingTail(offset, limit, totalItems, remainingTail,
                        excludedItemsNumberFromStartDate, nbOfItemsInTimeUnit, nbOfItemsToIgnore,
                        timeUnitsAreEqual.apply(date,dateTimeFrom));
            }
            totalItems += nbOfItemsInTimeUnit - nbOfItemsToIgnore;
        }

        TimeBasedPaginationUtil.sortListByDateDescending(itemsList, getAttributeToCompare);
        itemsList = TimeBasedPaginationUtil.removeExtraItemsFromList(itemsList, remainingOffset, remainingTail);

        return new ListResult<>(limit, offset, totalItems, itemsList);

    }

    private static boolean isTimeUnitWithItemsToAddToList(long offset, int limit, int totalItems,
            int nbOfItemsInTimeUnit, int nbOfItemsToIgnore) {
        return totalItems + nbOfItemsInTimeUnit - nbOfItemsToIgnore > offset && totalItems < offset + limit;
    }

    private static int getNbOfItemsToIgnore(ZonedDateTime dateTimeFrom, ZonedDateTime dateTimeTo,
            BiFunction<ZonedDateTime, ZonedDateTime, Boolean> timeUnitsAreEqual, int excludedItemsNumberFromStartDate,
            int excludedItemsNumberFromEndDate, ZonedDateTime date) {
        int nbOfItemsToIgnore = 0;
        if (timeUnitsAreEqual.apply(date, dateTimeTo)) {
            nbOfItemsToIgnore += excludedItemsNumberFromEndDate;
        }
        if (timeUnitsAreEqual.apply(date, dateTimeFrom)) {
            nbOfItemsToIgnore += excludedItemsNumberFromStartDate;
        }
        return nbOfItemsToIgnore;
    }

    /**
     * @param list the full list of items
     * @param offset the offset before which to remove items
     * @param tail the number of items to remove at the end of the list
     * @param <T> the type of the items in the list
     * @return a new list with items removed based on offset and tail
     */
    public static <T> List<T> removeExtraItemsFromList(List<T> list, int offset, int tail) {
        List<T> result = list;
        // remove remaining items that we don't need from beginning and end of list
        if (offset > 0 && result.size() >= offset) {
            result = result.subList(offset, result.size());
        }
        if (tail > 0 && result.size() >= tail) {
            result = result.subList(0, result.size() - tail);
        }
        return result;
    }

    /**
     * @param list the list to be sorted
     * @param getAttributeToCompare - given one of the items in question, this function must return a datetime based on
     *                              the item or it's attributes that is to be used to compare and sort the items (e.g.
     *                              it can be the item's creation datetime, or any other similar datetime attribute)
     * @param <T> the type of the items in the list
     */
    public static <T> void sortListByDateDescending(List<T> list, Function<T, ZonedDateTime> getAttributeToCompare) {
        // sort by date, descending
        list.sort((o1, o2) -> {
            ZonedDateTime endDateTime1 = getAttributeToCompare.apply(o1);
            ZonedDateTime endDateTime2 = getAttributeToCompare.apply(o2);
            // take into account that we might have null values as well :
            return ObjectUtils.compare(endDateTime2, endDateTime1, true);
        });
    }

    private static <T> int getNumberOfExcludedItems(List<T> list, Predicate<ZonedDateTime> zonedDateTimePredicate,
            Function<T, ZonedDateTime> getAttributeToCompare) {
        return (int) list.stream().map(getAttributeToCompare)
                .filter(zonedDateTimePredicate).count();
    }

    private static int updateRemainingOffset(long offset, int totalItems, int remainingOffset,
                                      int excludedItemsNumberFromEndOfInterval, boolean matchesEndOfInterval) {
        int result = remainingOffset;
        if (totalItems < offset) {
            result = (int) (offset - totalItems);
        }
        if (matchesEndOfInterval) {
            result += excludedItemsNumberFromEndOfInterval;
        }
        return result;
    }

    private static int updateRemainingTail(long offset, int limit, int totalItems, int remainingTail,
            int excludedItemsNumberFromStartOfInterval, int nbOfItemsInCurrentTimeUnit, int nbOfItemsToIgnore,
            boolean matchesStartOfInterval) {
        int result = remainingTail;
        if (totalItems + nbOfItemsInCurrentTimeUnit - nbOfItemsToIgnore > offset + limit) {
            result = (int) ((totalItems + nbOfItemsInCurrentTimeUnit - nbOfItemsToIgnore) - (offset + limit));
        }
        if (matchesStartOfInterval) {
            result += excludedItemsNumberFromStartOfInterval;
        }
        return result;
    }
}
