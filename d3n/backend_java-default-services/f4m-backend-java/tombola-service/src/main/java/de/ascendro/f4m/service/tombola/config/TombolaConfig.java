package de.ascendro.f4m.service.tombola.config;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;

public class TombolaConfig extends F4MConfigImpl {
    public static final String AEROSPIKE_TOMBOLA_SET = "tombola.aerospike.set";
    public static final String AEROSPIKE_TOMBOLA_TICKET_SET = "tombolaTicket.aerospike.set";
    public static final String AEROSPIKE_AVAILABLE_TOMBOLA_LIST_SET = "availableTombolaList.aerospike.set";
    public static final String AEROSPIKE_TOMBOLA_DRAWING_LIST_SET = "tombolaDrawingList.aerospike.set";
    public static final String AEROSPIKE_USER_TOMBOLA_SET = "userTombola.aerospike.set";

    public TombolaConfig() {
        super(new AerospikeConfigImpl());
        setProperty(AEROSPIKE_TOMBOLA_SET, "tombola");
        setProperty(AEROSPIKE_TOMBOLA_TICKET_SET, "tombolaTicket");
        setProperty(AEROSPIKE_AVAILABLE_TOMBOLA_LIST_SET, "availableTombolaList");
        setProperty(AEROSPIKE_TOMBOLA_DRAWING_LIST_SET, "tombolaDrawingList");
        setProperty(AEROSPIKE_USER_TOMBOLA_SET, "userTombola");

        loadProperties();
    }
}
