ALTER TABLE papers
    ADD COLUMN image_manifest_json LONGTEXT NULL AFTER raw_text;
