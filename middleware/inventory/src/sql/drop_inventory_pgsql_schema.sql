--
-- Copyright (c) 2013, by the California Institute of Technology.
-- ALL RIGHTS RESERVED. U.S. Government sponsorship acknowledged.
--
-- $Id: $
--

--
-- This SQL script drops the database objects for the HORIZON Inventory schema.
--

DROP TABLE IF EXISTS tie.collection CASCADE;
DROP TABLE IF EXISTS tie.collection_contact CASCADE;
DROP TABLE IF EXISTS tie.collection_imagery_set CASCADE;
DROP TABLE IF EXISTS tie.contact CASCADE;
DROP TABLE IF EXISTS tie.dataset CASCADE;
DROP TABLE IF EXISTS tie.dataset_imagery CASCADE;
DROP TABLE IF EXISTS tie.echo CASCADE;
DROP TABLE IF EXISTS tie.element_dd CASCADE;
DROP TABLE IF EXISTS tie.granule CASCADE;
DROP TABLE IF EXISTS tie.granule_imagery CASCADE;
DROP TABLE IF EXISTS tie.ogc_layer CASCADE;
DROP TABLE IF EXISTS tie.ogc_layer_bbox CASCADE;
DROP TABLE IF EXISTS tie.ogc_layer_dimension CASCADE;
DROP TABLE IF EXISTS tie.ogc_layer_matrix CASCADE;
DROP TABLE IF EXISTS tie.ogc_tile_matrix CASCADE;
DROP TABLE IF EXISTS tie.ogc_tile_matrix_set CASCADE;
DROP TABLE IF EXISTS tie.product CASCADE;
DROP TABLE IF EXISTS tie.product_archive CASCADE;
DROP TABLE IF EXISTS tie.product_archive_reference CASCADE;
DROP TABLE IF EXISTS tie.product_data_day CASCADE;
DROP TABLE IF EXISTS tie.product_character CASCADE;
DROP TABLE IF EXISTS tie.product_contact CASCADE;
DROP TABLE IF EXISTS tie.product_datetime CASCADE;
DROP TABLE IF EXISTS tie.product_element CASCADE;
DROP TABLE IF EXISTS tie.product_integer CASCADE;
DROP TABLE IF EXISTS tie.product_meta_history CASCADE;
DROP TABLE IF EXISTS tie.product_operation CASCADE;
DROP TABLE IF EXISTS tie.product_real CASCADE;
DROP TABLE IF EXISTS tie.product_reference CASCADE;
DROP TABLE IF EXISTS tie.product_type CASCADE;
DROP TABLE IF EXISTS tie.product_type_character CASCADE;
DROP TABLE IF EXISTS tie.product_type_coverage CASCADE;
DROP TABLE IF EXISTS tie.product_type_datetime CASCADE;
DROP TABLE IF EXISTS tie.product_type_element CASCADE;
DROP TABLE IF EXISTS tie.product_type_integer CASCADE;
DROP TABLE IF EXISTS tie.product_type_location_policy CASCADE;
DROP TABLE IF EXISTS tie.product_type_metadata CASCADE;
DROP TABLE IF EXISTS tie.product_type_policy CASCADE;
DROP TABLE IF EXISTS tie.product_type_real CASCADE;
DROP TABLE IF EXISTS tie.product_type_resource CASCADE;
DROP TABLE IF EXISTS tie.projection CASCADE;
DROP TABLE IF EXISTS tie.provider CASCADE;
DROP TABLE IF EXISTS tie.provider_resource CASCADE;
DROP TABLE IF EXISTS tie.product_type_generation CASCADE;


DROP SEQUENCE IF EXISTS tie.hibernate_sequence;
DROP SEQUENCE IF EXISTS tie.collection_id_seq;
DROP SEQUENCE IF EXISTS tie.collection_contact_id_seq;
DROP SEQUENCE IF EXISTS tie.collection_imagery_set_id_seq;
DROP SEQUENCE IF EXISTS tie.contact_id_seq;
DROP SEQUENCE IF EXISTS tie.dataset_id_seq;
DROP SEQUENCE IF EXISTS tie.dataset_imagery_id_seq;
DROP SEQUENCE IF EXISTS tie.echo_id_seq;
DROP SEQUENCE IF EXISTS tie.element_dd_id_seq;
DROP SEQUENCE IF EXISTS tie.granule_id_seq;
DROP SEQUENCE IF EXISTS tie.granule_imagery_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_layer_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_layer_bbox_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_layer_dimension_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_layer_matrix_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_tile_matrix_id_seq;
DROP SEQUENCE IF EXISTS tie.ogc_tile_matrix_set_id_seq;
DROP SEQUENCE IF EXISTS tie.product_id_seq;
DROP SEQUENCE IF EXISTS tie.product_archive_id_seq;
DROP SEQUENCE IF EXISTS tie.product_archive_reference_id_seq;
DROP SEQUENCE IF EXISTS tie.product_data_day_id_seq;
DROP SEQUENCE IF EXISTS tie.product_character_id_seq;
DROP SEQUENCE IF EXISTS tie.product_contact_id_seq;
DROP SEQUENCE IF EXISTS tie.product_datetime_id_seq;
DROP SEQUENCE IF EXISTS tie.product_element_id_seq;
DROP SEQUENCE IF EXISTS tie.product_integer_id_seq;
DROP SEQUENCE IF EXISTS tie.product_meta_history_id_seq;
DROP SEQUENCE IF EXISTS tie.product_operation_id_seq;
DROP SEQUENCE IF EXISTS tie.product_real_id_seq;
DROP SEQUENCE IF EXISTS tie.product_reference_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_character_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_coverage_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_datetime_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_element_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_integer_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_location_policy_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_metadata_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_policy_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_real_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_resource_id_seq;
DROP SEQUENCE IF EXISTS tie.projection_id_seq;
DROP SEQUENCE IF EXISTS tie.provider_id_seq;
DROP SEQUENCE IF EXISTS tie.provider_resource_id_seq;
DROP SEQUENCE IF EXISTS tie.product_type_generation_id_seq;
