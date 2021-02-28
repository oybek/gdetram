
create extension fuzzystrmatch;

create type platform as enum('vk', 'tg');

create table city (
    id          serial primary key,
    name        varchar not null,
    latitude    real not null,
    longitude   real not null
);

insert into city (id, name, latitude, longitude) values
    (1, 'екатеринбург', 56.833332, 60.583332),
    (2, 'казань', 55.7887383, 49.122139),
    (3, 'пермь', 58.1130346, 56.2789048),
    (4, 'хельсинки', 60.1695, 24.9354);

create table stop (
    id        serial primary key,
    name      varchar not null,
    latitude  real not null,
    longitude real not null,
    url       varchar not null,
    city_id   integer references city(id) not null
);

create table journal (
    stop_id  integer references stop(id),
    time     timestamp not null,
    user_id  varchar not null,
    text     varchar not null,
    platform platform not null
);

create table usr (
    platform    platform not null,
    id          integer not null,
    city_id     integer not null references city(id),
    primary key (platform, id)
);

create table sync_message (
    platform Platform not null,
    id       bigint not null,
    text     varchar not null
);

create table async_message (
    platform Platform not null,
    id       bigint not null,
    text     varchar not null
);
