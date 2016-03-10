--
-- Copyright (c) 2013, by the California Institute of Technology.
-- ALL RIGHTS RESERVED. U.S. Government sponsorship acknowledged.
--
-- $Id: $
--

--
-- This SQL script creates the database objects for the HORIZON Inventory schema.
--

CREATE TABLE tie.collection
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  aggregate          boolean NOT NULL,
  description        character varying(4000) NOT NULL,
  full_description   text,
  long_name          character varying(1024) NOT NULL,
  short_name         character varying(80) NOT NULL,
  
  CONSTRAINT collection_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.collection_contact
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  collection_id      bigint NOT NULL,
  contact_id         bigint NOT NULL,
  
  CONSTRAINT collection_contact_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.collection_imagery_set
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  collection_id      bigint NOT NULL,
  pt_id              bigint NOT NULL,
  
  CONSTRAINT collection_imagery_set_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.contact
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  address            character varying(512),
  email              character varying(255) NOT NULL,
  fax                character varying(80),
  first_name         character varying(80) NOT NULL,
  last_name          character varying(80) NOT NULL,
  middle_name        character varying(80),
  notify_type        character varying(20),
  phone              character varying(80),
  provider_id        bigint NOT NULL,
  role               character varying(40) NOT NULL,
  
  CONSTRAINT contact_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.dataset
(
  id                 bigint NOT NULL,
  revision           bigint NOT NULL,
  description        text,
  long_name          character varying(255) NOT NULL,
  metadata_endpoint  character varying(255),
  metadata_registry  character varying(255),
  provider_id        bigint,
  remote_dataset_id  character varying(160) NOT NULL,
  short_name         character varying(160) NOT NULL,
  version            character varying(255) NOT NULL,
  
  CONSTRAINT dataset_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.dataset_imagery
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  dataset_id         bigint NOT NULL,
  pt_id              bigint NOT NULL,
  
  CONSTRAINT dataset_imagery_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.echo
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  base_url           character varying(255) NOT NULL,
  env                character varying(255) NOT NULL,
  
  CONSTRAINT echo_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.element_dd
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(255),
  long_name          character varying(255),
  max_length         integer,
  scope              character varying(30),
  short_name         character varying(255) NOT NULL,
  type               character varying(30) NOT NULL,
  
  CONSTRAINT element_dd_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.granule
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  dataset_id         bigint NOT NULL,
  metadata_endpoint  character varying(255),
  remote_granule_ur  character varying(255),
  
  CONSTRAINT granule_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.granule_imagery
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  granule_id         bigint NOT NULL,
  product_id         bigint NOT NULL,
  
  CONSTRAINT granule_imagery_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.ogc_layer
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  format             character varying(255),
  pt_id              bigint NOT NULL,
  title              character varying(255),
  
  CONSTRAINT ogc_layer_pkey PRIMARY KEY (id)
);


CREATE TABLE tie.ogc_layer_bbox
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  layer_id           bigint NOT NULL,
  lower_corner       character varying(255) NOT NULL,
  projection_id      bigint NOT NULL,
  upper_corner       character varying(255) NOT NULL,
  
  CONSTRAINT ogc_layer_bbox_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.ogc_layer_dimension
(
  id                bigint NOT NULL,
  version           bigint NOT NULL,
  abstract_string   character varying(255) NOT NULL,
  current           boolean NOT NULL,
  default_string    character varying(255) NOT NULL,
  identifier        character varying(255) NOT NULL,
  keywords          character varying(255) NOT NULL,
  layer_id          bigint NOT NULL,
  title             character varying(255) NOT NULL,
  unit_symbol       character varying(255) NOT NULL,
  uom               character varying(255) NOT NULL,
  value             character varying(255) NOT NULL,
  
  CONSTRAINT ogc_layer_dimension_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.ogc_layer_matrix
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  layer_id           bigint NOT NULL,
  matrix_id          bigint NOT NULL,
  
  CONSTRAINT ogc_layer_matrix_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.ogc_tile_matrix
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  identifier         character varying(255) NOT NULL,
  matrix_height      integer NOT NULL,
  matrix_width       integer NOT NULL,
  scale_denominator  numeric(19,2) NOT NULL,
  tile_height        integer NOT NULL,
  tile_width         integer NOT NULL,
  tms_id             bigint NOT NULL,
  top_left_cornerx   integer NOT NULL,
  top_left_cornery   integer NOT NULL,
  
  CONSTRAINT ogc_tile_matrix_pkey PRIMARY KEY (id)
);
   

CREATE TABLE tie.ogc_tile_matrix_set
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  identifier         character varying(255) NOT NULL,
  
  CONSTRAINT ogc_tile_matrix_set_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product
(
  id                 bigint NOT NULL,
  revision           bigint NOT NULL,
  archive_time       bigint,
  create_time        bigint,
  name               character varying(255) NOT NULL,
  pt_id              bigint NOT NULL,
  rel_path           character varying(255),
  root_path          character varying(255),
  start_time         bigint NOT NULL,
  status             character varying(255) NOT NULL,
  stop_time          bigint,
  partial_id		 character varying(255),
  version            integer NOT NULL,
  
  CONSTRAINT product_pkey PRIMARY KEY (id)
  UNIQUE (pt_id, name)
);

CREATE TABLE tie.product_archive
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  checksum           character varying(255),
  compress_flag      boolean NOT NULL,
  file_size          bigint NOT NULL,
  name               character varying(255) NOT NULL,
  product_id         bigint NOT NULL,
  status             character varying(30) NOT NULL,
  type               character varying(30) NOT NULL,
  
  CONSTRAINT product_archive_pkey PRIMARY KEY (id)
  UNIQUE (product_id, name)
);

CREATE TABLE tie.product_archive_reference
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(1024),
  name               character varying(255) NOT NULL,
  path               character varying(1024) NOT NULL,
  product_archive_id bigint NOT NULL,
  status             character varying(10) NOT NULL,
  type               character varying(31),
  
  CONSTRAINT product_archive_reference_pkey PRIMARY KEY (id)
  UNIQUE (product_archive_id, name)
);


CREATE TABLE tie.product_data_day
(
  id 				bigint 	NOT NULL,
  version 			bigint 	NOT NULL,
  data_day 			bigint 	NOT NULL,
  product_id 		bigint 	NOT NULL,
  
  CONSTRAINT product_data_day_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_character
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pe_id              bigint NOT NULL,
  product_id         bigint NOT NULL,
  value              character varying(255) NOT NULL,
  
  CONSTRAINT product_character_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_contact
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  contact_id         bigint NOT NULL,
  product_id         bigint NOT NULL,
  
  CONSTRAINT product_contact_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_datetime
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pe_id              bigint NOT NULL,
  product_id         bigint NOT NULL,
  value_long         bigint NOT NULL,
  
  CONSTRAINT product_datetime_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_element
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  element_id         bigint NOT NULL,
  obligation_flag    boolean NOT NULL,
  product_id         bigint NOT NULL,
  scope              character varying(20) NOT NULL,
  
  CONSTRAINT product_element_pkey PRIMARY KEY (id)
);
   
CREATE TABLE tie.product_integer
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pe_id              bigint NOT NULL,
  product_id         bigint NOT NULL,
  units              character varying(10) NOT NULL,
  value              integer NOT NULL,
  
  CONSTRAINT product_integer_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_meta_history
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  creation_date      bigint NOT NULL,
  last_revision_date bigint NOT NULL,
  product_id         bigint NOT NULL,
  revision_history   character varying(255) NOT NULL,
  version_id         integer NOT NULL,
  
  CONSTRAINT product_meta_history_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_operation
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  agent              character varying(255) NOT NULL,
  arguments          character varying(255),
  command            character varying(255),
  operation          character varying(255) NOT NULL,
  product_id         bigint NOT NULL,
  start_time         bigint NOT NULL,
  stop_time          bigint NOT NULL,
  
  CONSTRAINT product_operation_pkey PRIMARY KEY (id)
);


CREATE TABLE tie.product_real
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pe_id              bigint NOT NULL,
  product_id         bigint NOT NULL,
  units              character varying(20) NOT NULL,
  value              numeric(19,2) NOT NULL,
  
  CONSTRAINT product_real_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_reference
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(1024),
  name               character varying(255),
  path               character varying(1024) NOT NULL,
  product_id         bigint NOT NULL,
  status             character varying(10) NOT NULL,
  type               character varying(31),
  
  CONSTRAINT product_reference_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  provider_id        bigint,
  description        text,
  identifier         character varying(160) NOT NULL,
  purgable           boolean,
  purge_rate         integer,
  title              character varying(255) NOT NULL,
  last_updated       bigint NOT NULL,
  
  CONSTRAINT product_type_pkey PRIMARY KEY (id)
  UNIQUE (identifier)
);

CREATE TABLE tie.product_type_character
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pt_id              bigint NOT NULL,
  pte_id             bigint NOT NULL,
  value              character varying(255) NOT NULL,
  
  CONSTRAINT product_type_character_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_coverage
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  east_longitude     real,
  north_latitude     real,
  pt_id              bigint NOT NULL,
  south_latitude     real,
  start_time         bigint,
  stop_time          bigint,
  west_longitude     real,
  
  CONSTRAINT product_type_coverage_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_datetime
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pt_id              bigint NOT NULL,
  pte_id             bigint NOT NULL,
  value_long         bigint NOT NULL,
  
  CONSTRAINT product_type_datetime_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_element
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  element_id         bigint NOT NULL,
  obligation_flag    boolean NOT NULL,
  pt_id              bigint NOT NULL,
  scope              character varying(20) NOT NULL,
  
  CONSTRAINT product_type_element_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_integer
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pt_id              bigint NOT NULL,
  pte_id             bigint NOT NULL,
  units              character varying(20) NOT NULL,
  value              integer NOT NULL,
  
  CONSTRAINT product_type_integer_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_location_policy
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  base_path          character varying(1024) NOT NULL,
  pt_id              bigint NOT NULL,
  type               character varying(30) NOT NULL,
  
  CONSTRAINT product_type_location_policy_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_metadata
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  asc_desc           character varying(255),
  science_parameter  character varying(255),
  data_version       character varying(255),
  day_night          character varying(255),
  display_resolution integer,
  instrument         character varying(255) NOT NULL,
  native_resolution  integer,
  platform           character varying(255) NOT NULL,
  processing_level   character varying(255),
  project            character varying(255) NOT NULL,
  source_projection_id  bigint,
  target_projection_id  bigint,
  pt_id              bigint NOT NULL,
  region_coverage    character varying(255),
  
  CONSTRAINT product_type_metadata_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_policy
(
  id                    bigint NOT NULL,
  version               bigint NOT NULL,
  access_constraint     character varying(1024) NOT NULL,
  access_type           character varying(20) NOT NULL,
  base_path_append_type character varying(20) NOT NULL,
  checksum_type         character varying(20) NOT NULL,
  compress_type         character varying(20) NOT NULL,
  data_class            character varying(20),
  data_duration         integer,
  data_format           character varying(20) NOT NULL,
  data_frequency        integer,
  data_latency          integer,
  data_volume           integer,
  delivery_rate         integer,
  multi_day             integer,
  multi_day_link        boolean,
  pt_id                 bigint NOT NULL,
  spatial_type          character varying(20) NOT NULL,
  use_constraint        character varying(1024) NOT NULL,
  
  CONSTRAINT product_type_policy_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_real
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  pt_id              bigint NOT NULL,
  pte_id             bigint NOT NULL,
  units              character varying(20) NOT NULL,
  value              numeric(19,2) NOT NULL,
  
  CONSTRAINT product_type_real_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_resource
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(255),
  name               character varying(160) NOT NULL,
  path               character varying(1024) NOT NULL,
  pt_id              bigint NOT NULL,
  type               character varying(255) NOT NULL,
  
  CONSTRAINT product_type_resource_pkey PRIMARY KEY (id)
);

ALTER TABLE tie.product_type_resource ADD 
   CONSTRAINT product_type_resource_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

CREATE TABLE tie.projection
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(1024) NOT NULL,
  epsg_code          character varying(30) NOT NULL,
  name               character varying(255) NOT NULL,
  native_bounds      character varying(255) NOT NULL,
  ogc_crs            character varying(255) NOT NULL,
  wg84bounds         character varying(255) NOT NULL,
  
  CONSTRAINT projection_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.provider
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  long_name          character varying(255) NOT NULL,
  short_name         character varying(160) NOT NULL,
  type               character varying(20) NOT NULL,
  
  CONSTRAINT provider_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.provider_resource
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  description        character varying(255),
  name               character varying(255) NOT NULL,
  path               character varying(255) NOT NULL,
  provider_id        bigint NOT NULL,
  type               character varying(255) NOT NULL,
  
  CONSTRAINT provider_resource_pkey PRIMARY KEY (id)
);

CREATE TABLE tie.product_type_generation
(
  id                 bigint NOT NULL,
  version            bigint NOT NULL,
  mrf_block_size     integer,
  output_sizex       integer,
  output_sizey       integer,
  overview_levels    integer,
  overview_resample  character varying(40),
  overview_scale     integer,
  pt_id              bigint NOT NULL,
  reprojection_resample character varying(40),
  resize_resample    character varying(40),
  vrt_nodata         character varying(255),
  
  CONSTRAINT product_type_generation_pkey PRIMARY KEY (id)
);


ALTER TABLE tie.collection_contact ADD 
   CONSTRAINT collection_contact_fk1
   FOREIGN KEY(contact_id) 
   REFERENCES tie.contact(id);

ALTER TABLE tie.collection_contact ADD 
   CONSTRAINT collection_contact_fk2
   FOREIGN KEY(collection_id) 
   REFERENCES tie.collection(id);
   
ALTER TABLE tie.collection_imagery_set ADD 
   CONSTRAINT collection_imagery_set_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);
   
ALTER TABLE tie.collection_imagery_set ADD 
   CONSTRAINT collection_imagery_set_fk2
   FOREIGN KEY(collection_id) 
   REFERENCES tie.collection(id);

ALTER TABLE tie.contact ADD 
   CONSTRAINT contact_fk1
   FOREIGN KEY(provider_id) 
   REFERENCES tie.provider(id);

ALTER TABLE tie.dataset ADD 
   CONSTRAINT dataset_fk1
   FOREIGN KEY(provider_id) 
   REFERENCES tie.provider(id);

ALTER TABLE tie.dataset_imagery ADD 
   CONSTRAINT dataset_imagery_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);
   
ALTER TABLE tie.dataset_imagery ADD 
   CONSTRAINT dataset_imagery_fk2
   FOREIGN KEY(dataset_id) 
   REFERENCES tie.dataset(id);

ALTER TABLE tie.granule ADD 
   CONSTRAINT granule_fk1
   FOREIGN KEY(dataset_id) 
   REFERENCES tie.dataset(id);

ALTER TABLE tie.granule_imagery ADD 
   CONSTRAINT granule_imagery_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);
   
ALTER TABLE tie.granule_imagery ADD 
   CONSTRAINT granule_imagery_fk2
   FOREIGN KEY(granule_id) 
   REFERENCES tie.granule(id);

ALTER TABLE tie.ogc_layer ADD 
   CONSTRAINT ogc_layer_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);
   
ALTER TABLE tie.ogc_layer_bbox ADD 
   CONSTRAINT ogc_layer_bbox_fk1
   FOREIGN KEY(layer_id) 
   REFERENCES tie.ogc_layer(id);
   
ALTER TABLE tie.ogc_layer_bbox ADD 
   CONSTRAINT ogc_layer_bbox_fk2
   FOREIGN KEY(projection_id) 
   REFERENCES tie.projection(id);
   
ALTER TABLE tie.ogc_layer_dimension ADD 
   CONSTRAINT ogc_layer_dimension_fk1
   FOREIGN KEY(layer_id) 
   REFERENCES tie.ogc_layer(id);

ALTER TABLE tie.ogc_layer_matrix ADD 
   CONSTRAINT ogc_layer_matrix_fk1
   FOREIGN KEY(layer_id) 
   REFERENCES tie.ogc_layer(id);
   
ALTER TABLE tie.ogc_layer_matrix ADD 
   CONSTRAINT ogc_layer_matrix_fk2
   FOREIGN KEY(matrix_id) 
   REFERENCES tie.ogc_tile_matrix_set(id);
   
ALTER TABLE tie.ogc_tile_matrix ADD 
   CONSTRAINT ogc_tile_matrix_fk1
   FOREIGN KEY(tms_id) 
   REFERENCES tie.ogc_tile_matrix_set(id);

ALTER TABLE tie.product ADD 
   CONSTRAINT product_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_archive ADD 
   CONSTRAINT product_archive_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_archive_reference ADD 
   CONSTRAINT product_archive_reference_fk1
   FOREIGN KEY(product_archive_id) 
   REFERENCES tie.product_archive(id);
   
ALTER TABLE tie.product_data_day ADD 
   CONSTRAINT product_data_day_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_character ADD 
   CONSTRAINT product_character_fk1
   FOREIGN KEY(pe_id) 
   REFERENCES tie.product_element(id);
   
ALTER TABLE tie.product_character ADD 
   CONSTRAINT product_character_fk2
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_contact ADD 
   CONSTRAINT product_contact_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);
   
ALTER TABLE tie.product_contact ADD 
   CONSTRAINT product_contact_fk2
   FOREIGN KEY(contact_id) 
   REFERENCES tie.contact(id);

ALTER TABLE tie.product_datetime ADD 
   CONSTRAINT product_datetime_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);
   
ALTER TABLE tie.product_datetime ADD 
   CONSTRAINT product_datetime_fk2
   FOREIGN KEY(pe_id) 
   REFERENCES tie.product_element(id);
   
ALTER TABLE tie.product_element ADD 
   CONSTRAINT product_element_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);
   
ALTER TABLE tie.product_element ADD 
   CONSTRAINT product_element_fk2
   FOREIGN KEY(element_id) 
   REFERENCES tie.element_dd(id);

ALTER TABLE tie.product_integer ADD 
   CONSTRAINT product_integer_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_integer ADD 
   CONSTRAINT product_integer_fk2
   FOREIGN KEY(pe_id) 
   REFERENCES tie.product_element(id);

ALTER TABLE tie.product_meta_history ADD 
   CONSTRAINT product_meta_history_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_operation ADD 
   CONSTRAINT product_operation_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_real ADD 
   CONSTRAINT product_real_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_real ADD 
   CONSTRAINT product_real_fk2
   FOREIGN KEY(pe_id) 
   REFERENCES tie.product_element(id);

ALTER TABLE tie.product_reference ADD 
   CONSTRAINT product_reference_fk1
   FOREIGN KEY(product_id) 
   REFERENCES tie.product(id);

ALTER TABLE tie.product_type ADD 
   CONSTRAINT product_type_fk1
   FOREIGN KEY(provider_id) 
   REFERENCES tie.provider(id);

ALTER TABLE tie.product_type_character ADD 
   CONSTRAINT product_type_character_fk1
   FOREIGN KEY(pte_id) 
   REFERENCES tie.product_type_element(id);

ALTER TABLE tie.product_type_character ADD 
   CONSTRAINT product_type_character_fk2
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_coverage ADD 
   CONSTRAINT product_type_coverage_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_datetime ADD 
   CONSTRAINT product_type_datetime_fk1
   FOREIGN KEY(pte_id) 
   REFERENCES tie.product_type_element(id);
   
ALTER TABLE tie.product_type_datetime ADD 
   CONSTRAINT product_type_datetime_fk2
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_element ADD 
   CONSTRAINT product_type_element_fk1
   FOREIGN KEY(element_id) 
   REFERENCES tie.element_dd(id);
   
ALTER TABLE tie.product_type_element ADD 
   CONSTRAINT product_type_element_fk2
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_integer ADD 
   CONSTRAINT product_type_integer_fk1
   FOREIGN KEY(pte_id) 
   REFERENCES tie.product_type_element(id);

ALTER TABLE tie.product_type_integer ADD 
   CONSTRAINT product_type_integer_fk2
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_location_policy ADD 
   CONSTRAINT product_type_location_policy_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_metadata ADD 
   CONSTRAINT product_type_metadata_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_policy ADD 
   CONSTRAINT product_type_policy_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_real ADD 
   CONSTRAINT product_type_real_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

ALTER TABLE tie.product_type_real ADD 
   CONSTRAINT product_type_real_fk2
   FOREIGN KEY(pte_id) 
   REFERENCES tie.product_type_element(id);

ALTER TABLE tie.provider_resource ADD 
   CONSTRAINT provider_resource_fk1
   FOREIGN KEY(provider_id) 
   REFERENCES tie.provider(id);
   
ALTER TABLE tie.product_type_generation ADD 
   CONSTRAINT product_type_generation_fk1
   FOREIGN KEY(pt_id) 
   REFERENCES tie.product_type(id);

CREATE SEQUENCE tie.collection_id_seq
   CACHE 1;
CREATE SEQUENCE tie.collection_contact_id_seq
   CACHE 1;
CREATE SEQUENCE tie.collection_imagery_set_id_seq
   CACHE 1;
CREATE SEQUENCE tie.contact_id_seq
   CACHE 1;
CREATE SEQUENCE tie.dataset_id_seq
   CACHE 1;
CREATE SEQUENCE tie.dataset_imagery_id_seq
   CACHE 1;
CREATE SEQUENCE tie.echo_id_seq
   CACHE 1;
CREATE SEQUENCE tie.element_dd_id_seq
   CACHE 1;
CREATE SEQUENCE tie.granule_id_seq
   CACHE 1;
CREATE SEQUENCE tie.granule_imagery_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_layer_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_layer_bbox_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_layer_dimension_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_layer_matrix_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_tile_matrix_id_seq
   CACHE 1;
CREATE SEQUENCE tie.ogc_tile_matrix_set_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_archive_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_archive_reference_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_data_day_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_character_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_contact_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_datetime_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_element_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_integer_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_meta_history_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_operation_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_real_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_reference_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_character_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_coverage_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_datetime_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_element_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_integer_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_location_policy_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_metadata_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_policy_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_real_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_resource_id_seq
   CACHE 1;
CREATE SEQUENCE tie.projection_id_seq
   CACHE 1;
CREATE SEQUENCE tie.provider_id_seq
   CACHE 1;
CREATE SEQUENCE tie.provider_resource_id_seq
   CACHE 1;
CREATE SEQUENCE tie.product_type_generation_id_seq
   CACHE 1;
