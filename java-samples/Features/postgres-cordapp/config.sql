
create USER "party_a" WITH LOGIN PASSWORD 'test';
create SCHEMA "party_a_schema";

-- allow the user to access the schema and create objects in that schema.
grant USAGE, create ON SCHEMA "party_a_schema" TO "party_a";

-- adding permissions for current tables in that schema and the tables created in the future.
grant select, insert, update, delete, REFERENCES ON ALL tables IN SCHEMA "party_a_schema" TO "party_a";
alter DEFAULT privileges IN SCHEMA "party_a_schema" grant select, insert, update, delete, REFERENCES ON tables TO "party_a";
grant USAGE, select ON ALL sequences IN SCHEMA "party_a_schema" TO "party_a";
alter DEFAULT privileges IN SCHEMA "party_a_schema" grant USAGE, select ON sequences TO "party_a";
alter role "party_a" SET search_path = "party_a_schema";


-- doing the same for party_b
create USER "party_b" with LOGIN PASSWORD 'test';
create SCHEMA "party_b_schema";
grant USAGE, create ON SCHEMA "party_b_schema" TO "party_b";
grant select, insert, update, delete, REFERENCES ON ALL tables IN SCHEMA "party_b_schema" TO "party_b";
alter DEFAULT privileges IN SCHEMA "party_b_schema" grant select, insert, update, delete, REFERENCES ON tables TO "party_b";
grant USAGE, select ON ALL sequences IN SCHEMA "party_b_schema" TO "party_b";
alter DEFAULT privileges IN SCHEMA "party_b_schema" grant USAGE, select ON sequences TO "party_b";
alter role "party_b" SET search_path = "party_b_schema";
