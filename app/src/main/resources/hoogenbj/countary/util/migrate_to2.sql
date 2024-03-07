create table transactions_dg_tmp (id INTEGER primary key autoincrement, accountId INTEGER references account on delete cascade, txdate INTEGER, amount NUMERIC, balance NUMERIC, description TEXT, hash TEXT, allocated integer default 0, posting_date integer not null, manual integer default 0);
insert into transactions_dg_tmp(id, accountId, txdate, amount, balance, description, hash, allocated, posting_date, manual) select id, accountId, txdate, amount, balance, description, hash, allocated, posting_date, manual from transactions;
drop table transactions;
alter table transactions_dg_tmp rename to transactions;
create index transactions_hash_index on transactions (hash);
CREATE TRIGGER transactions_ad AFTER DELETE ON transactions BEGIN INSERT INTO transactions_idx(transactions_idx, rowid, accountId, txdate, amount, balance, description, posting_date, allocated, manual) VALUES('delete', old.id, old.accountId, old.txdate, old.amount, old.balance, old.description, old.posting_date, old.allocated, old.manual); END;
CREATE TRIGGER transactions_ai AFTER INSERT ON transactions BEGIN INSERT INTO transactions_idx(rowid, accountId, txdate, amount, balance, description, posting_date, allocated, manual) VALUES (new.id, new.accountId, new.txdate, new.amount, new.balance, new.description, new.posting_date, new.allocated, new.manual); END;
CREATE TRIGGER transactions_au AFTER UPDATE ON transactions BEGIN INSERT INTO transactions_idx(transactions_idx, rowid, accountId, txdate, amount, balance, description, posting_date, allocated, manual) VALUES('delete', old.id, old.accountId, old.txdate, old.amount, old.balance, old.description, old.posting_date, old.allocated, old.manual); INSERT INTO transactions_idx(rowid, accountId, txdate, amount, balance, description, posting_date, allocated, manual) VALUES (new.id, new.accountId, new.txdate, new.amount, new.balance, new.description, new.posting_date, new.allocated, new.manual); END;
create table db_version (id integer not null constraint db_version_pk primary key, version integer not null);
insert into db_version (id,version) values (1,2);
