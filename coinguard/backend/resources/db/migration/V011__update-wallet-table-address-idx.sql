drop index if exists wallet_address_idx;
create index wallet_address_idx on wallet ((data->>'address'));
