package nl.futureedge.simple.jta.store.jdbc.sql;

public class HsqldbSqlTemplate extends DefaultSqlTemplate {

    public HsqldbSqlTemplate() {
        setSelectNextTransactionId("call next value for transaction_seq");

        setCreateResourceTable("create table transaction_resources(\n"
                + "    transaction_id bigint not null,\n"
                + "    name varchar(30) not null,\n"
                + "    status varchar(30) not null,\n"
                + "    cause clob,\n"
                + "    created timestamp not null,\n"
                + "    updated timestamp not null\n"
                + ")");
    }
}
