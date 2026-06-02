-- V21: Seed Geography category with country population questions
-- Generated from restcountries.com/v3.1 data

-- Geography category
INSERT INTO categories (id, name, slug, description, created_at, updated_at) VALUES (
  '3d06f275-d476-6edf-132c-9cf74559a1a9', 'Geography', 'geography',
  'Country population trivia — name a country and score its population in millions',
  NOW(), NOW()
) ON CONFLICT (slug) DO NOTHING;

-- Geography questions (hand-curated, no template)
INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', '3d06f275-d476-6edf-132c-9cf74559a1a9',
  'Name a country — its population in millions is your score',
  'population_millions',
  '{"entity_type": "country"}'::jsonb,
  1, 2, 'draft', NULL, '{}'::jsonb,
  0, 0, 0, 0, 0, false, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  'a10296e6-6526-47a6-db85-a6fc81a0d51b', '3d06f275-d476-6edf-132c-9cf74559a1a9',
  'Name a country in Africa — its population in millions is your score',
  'population_millions',
  '{"entity_type": "country", "region": "Africa"}'::jsonb,
  1, 2, 'draft', NULL, '{}'::jsonb,
  0, 0, 0, 0, 0, false, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  'a64937b9-1e46-4d43-c599-00e623fd5e24', '3d06f275-d476-6edf-132c-9cf74559a1a9',
  'Name a country in Americas — its population in millions is your score',
  'population_millions',
  '{"entity_type": "country", "region": "Americas"}'::jsonb,
  1, 2, 'draft', NULL, '{}'::jsonb,
  0, 0, 0, 0, 0, false, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  '8a2b9539-5c83-ed99-181d-41fdb49365a7', '3d06f275-d476-6edf-132c-9cf74559a1a9',
  'Name a country in Asia — its population in millions is your score',
  'population_millions',
  '{"entity_type": "country", "region": "Asia"}'::jsonb,
  1, 2, 'draft', NULL, '{}'::jsonb,
  0, 0, 0, 0, 0, false, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, category_id, question_text, metric_key, config, min_score, difficulty, status, template_id, template_params, high_value_count, mid_range_count, checkout_count, total_valid_count, total_score_pool, single_question_viable, difficulty_score, difficulty_locked, suitable_for_daily, created_at, updated_at) VALUES (
  '6406745b-9110-bac2-f207-1c8f84100558', '3d06f275-d476-6edf-132c-9cf74559a1a9',
  'Name a country in Europe — its population in millions is your score',
  'population_millions',
  '{"entity_type": "country", "region": "Europe"}'::jsonb,
  1, 2, 'draft', NULL, '{}'::jsonb,
  0, 0, 0, 0, 0, false, 0.00, false, false,
  NOW(), NOW()
) ON CONFLICT (id) DO NOTHING;

-- Entities (country names for autocomplete)
-- Using ON CONFLICT DO NOTHING to handle duplicates across alt spellings
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6c948876-de9e-fa9d-aa38-a62765790feb', 'country', 'Afghanistan', 'afghanistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1312df9d-5137-d8d1-cd38-77c2e51a19d7', 'country', 'Afġānistān', 'afganistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cc134980-a03e-e42c-8b69-bacdc869b5b0', 'country', 'Al-Jumhūrīyah Al-Libnānīyah', 'al-jumhuriyah al-libnaniyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '203ea3e0-d855-0bdb-f743-bbdbda0724b9', 'country', 'Al-Jumhūrīyah Al-ʻArabīyah As-Sūrīyah', 'al-jumhuriyah al-ʻarabiyah as-suriyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5a2ac560-0e62-8095-ab6a-396d340d9493', 'country', 'Al-Mamlakah al-Maġribiyah', 'al-mamlakah al-magribiyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bb04dc39-c3f0-6307-851a-ddeaf7b9089d', 'country', 'Al-Mamlakah al-‘Arabiyyah as-Su‘ūdiyyah', 'al-mamlakah al-‘arabiyyah as-su‘udiyyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4716094d-2c83-7ede-88c8-36126525a1f5', 'country', 'Albania', 'albania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0156a7c4-3801-17a4-a0ca-6e19ea99188f', 'country', 'Algeria', 'algeria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e2f71608-ee3e-1c8c-5419-d62a249321ac', 'country', 'Algérie', 'algerie', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c6d36315-5a4d-7f90-e899-be56331184b4', 'country', 'Andorra', 'andorra', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ceb6c7d3-c39b-00d2-8e4e-11cdfe3b9fcc', 'country', 'Angola', 'angola', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1cfa130a-f991-e037-e99c-cbd6d0ba6d52', 'country', 'Antigua and Barbuda', 'antigua and barbuda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '44f046d8-0945-4e9e-b67e-c1de19131729', 'country', 'Aolepān Aorōkin M̧ajeļ', 'aolepan aorokin majel', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '048966ce-c7f1-bd0b-a056-d580b2195337', 'country', 'Aotearoa', 'aotearoa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '56463100-1825-8f6a-0682-7ae9cbbd59be', 'country', 'Arab Republic of Egypt', 'arab republic of egypt', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2b33e849-e2ba-ddb4-b19e-bf246d70ca17', 'country', 'Argentina', 'argentina', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a9742f1f-f994-912c-53fc-6e23f073ca77', 'country', 'Argentine Republic', 'argentine republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a1ebea8d-d99d-3def-f3bc-2f6338ef1ec7', 'country', 'Armenia', 'armenia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'eaa6bbfd-f3f2-e12f-4633-110ec80b28aa', 'country', 'Australia', 'australia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a20c66e5-5d70-240b-c9a4-e8784fb41d4d', 'country', 'Austria', 'austria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6c20a84e-cc32-ae23-398e-ff7d74f861ef', 'country', 'Azerbaijan', 'azerbaijan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '906f1edd-cdfa-e9c2-98c3-e459bdf87df1', 'country', 'Azərbaycan Respublikası', 'azərbaycan respublikası', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6b14a0fc-269b-7785-7cdd-0749ac2c8379', 'country', 'Bahamas', 'bahamas', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c207c8f-9856-4217-4d34-d08957efe66a', 'country', 'Bahrain', 'bahrain', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3ddf624e-71d8-f1b7-1533-e991fe46df4d', 'country', 'Bangladesh', 'bangladesh', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c146eaa5-038f-1b9b-bed3-b20477a75217', 'country', 'Barbados', 'barbados', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7f6039fc-6d0c-4df9-f2bb-b0fb0f5c3d24', 'country', 'Belarus', 'belarus', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '76a83b89-896f-48a1-572d-393430117b5e', 'country', 'Belgie', 'belgie', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '671189e5-fcc9-ae64-499a-6aeaff39408a', 'country', 'Belgien', 'belgien', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ebfa4e93-9df9-3020-9ed8-1178a0dda23e', 'country', 'Belgique', 'belgique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '20fd50f4-43c2-5f94-3d0f-823c92d0bd41', 'country', 'Belgium', 'belgium', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e8527e83-8081-82c8-7715-373f11cb5a95', 'country', 'België', 'belgie', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd0d3f63a-4bee-bc14-2f4b-6a1d9d338561', 'country', 'Belize', 'belize', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd8d47b1e-14cc-691b-113f-a02d1c198b30', 'country', 'Beluu er a Belau', 'beluu er a belau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'edf46b19-33fb-6168-c579-267d31bd8fc0', 'country', 'Benin', 'benin', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '57a3d494-d8d9-3486-cf15-c8a7b303b1d4', 'country', 'Bharat Ganrajya', 'bharat ganrajya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fd11e3d0-d7a7-a2e4-b77b-8386223509d2', 'country', 'Bhutan', 'bhutan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ee091bf3-1757-53ea-7b20-309e60a1899f', 'country', 'Bhārat', 'bharat', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5be611f6-eb93-fda0-cf7f-1181bb0c5292', 'country', 'Bielaruś', 'bielarus', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ee3ef859-33cb-3bcb-f716-764804596a52', 'country', 'Bolivarian Republic of', 'bolivarian republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '764abc8f-a588-62c0-930a-e204ca8653aa', 'country', 'Bolivarian Republic of Venezuela', 'bolivarian republic of venezuela', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'dff3598f-b982-af41-3434-0b0d3dd39c2c', 'country', 'Bolivia', 'bolivia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e3e0c62e-6a02-1379-7939-e34fdb534cd6', 'country', 'Bosnia and Herzegovina', 'bosnia and herzegovina', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '664669ac-ebe2-c837-ac2d-bc8231abdac8', 'country', 'Bosnia-Herzegovina', 'bosnia-herzegovina', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '14aebdd3-588d-6cb0-0ac7-f6dd880fbf21', 'country', 'Botswana', 'botswana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '508479aa-3e87-a132-1b8c-0d37c1260666', 'country', 'Brasil', 'brasil', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5b7a312d-f9e7-2f4b-0b1f-bb6f62a388b5', 'country', 'Brazil', 'brazil', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ce659de9-20c2-523d-379b-3e213725fd1c', 'country', 'Brunei', 'brunei', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6d9039fd-b921-b9cd-8f31-1c71f46e1c77', 'country', 'Brunei Darussalam', 'brunei darussalam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '41026010-2b12-469f-f41a-555908b090d0', 'country', 'Bulgaria', 'bulgaria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 50 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bb2e812b-57ab-caf3-0817-cdc7c46c509c', 'country', 'Buliwya', 'buliwya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2cfc8efd-ae1b-de72-643a-4426cde77e1f', 'country', 'Buliwya Mamallaqta', 'buliwya mamallaqta', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9e7dd0c7-5674-0b23-b50f-3c9387290b9a', 'country', 'Bundesrepublik Deutschland', 'bundesrepublik deutschland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f7086384-3044-838c-41c6-a4d8eaa3608b', 'country', 'Burkina Faso', 'burkina faso', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '15a323f7-2e69-6a37-33fa-84a560034e56', 'country', 'Burma', 'burma', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '42a00894-a3c5-f787-b5a5-db1a8fa94f4b', 'country', 'Burundi', 'burundi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5d3c47a9-b530-6552-5b48-e3257424d865', 'country', 'Cambodia', 'cambodia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c7367eb5-cd96-77f1-124e-1ae05589fe96', 'country', 'Cameroon', 'cameroon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '369cf172-1ec9-be0b-a953-60925d7569fe', 'country', 'Canada', 'canada', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f1834529-809a-6c0f-a0f1-b6166116582c', 'country', 'Cape Verde', 'cape verde', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1ffb8090-5597-0393-8885-34b25489c039', 'country', 'Central African Republic', 'central african republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '80dc48fd-bdf1-3e32-4050-0184ae2f3112', 'country', 'Chad', 'chad', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2acb1454-48ac-268c-cf96-1a6b7bcd37c5', 'country', 'Chile', 'chile', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bfffe8ba-7fc7-8186-9911-a443a702e716', 'country', 'China', 'china', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8bef2a12-7fdc-f647-b3d8-b064b7276329', 'country', 'Chosŏn Minjujuŭi Inmin Konghwaguk', 'choson minjujuui inmin konghwaguk', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1a9a20c2-b602-6c46-7c11-bfe3fa6d545f', 'country', 'Co-operative Republic of Guyana', 'co-operative republic of guyana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a43c2570-6061-008c-7870-68c3b4d9e4ab', 'country', 'Colombia', 'colombia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4db2afb5-3cd1-b9da-994b-e2eadae548e8', 'country', 'Commonwealth of Dominica', 'commonwealth of dominica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8b1748ae-609b-533f-3bec-7bf2b7b5bd26', 'country', 'Commonwealth of the Bahamas', 'commonwealth of the bahamas', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2e43dbcd-506a-6e48-e37c-dc4f7c59a2e7', 'country', 'Comoros', 'comoros', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3196a880-e909-52df-f6df-ef9bd82cb946', 'country', 'Congo', 'congo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '710b8b0d-6d68-9b94-a7e9-845d7a3a661c', 'country', 'Congo-Brazzaville', 'congo-brazzaville', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '79cc3428-0702-88b8-4bd4-ac5b6149cd63', 'country', 'Congo-Kinshasa', 'congo-kinshasa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c89ab98a-ec41-bb6e-b190-7bd0ce7e5117', 'country', 'Costa Rica', 'costa rica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e0e06ff3-87cd-0e54-ea26-6a8e3fbc576d', 'country', 'Crna Gora', 'crna gora', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '77496591-12a2-9fc6-3a71-c20d73786eea', 'country', 'Croatia', 'croatia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7cda4c0e-fc18-b5cb-a155-46191c27d2bb', 'country', 'Cuba', 'cuba', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '615bb75b-79cf-ab3f-b90a-c2ecd3d0f814', 'country', 'Cyprus', 'cyprus', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f011614b-38a3-cc0c-7349-7b8d6f2d3f07', 'country', 'Czechia', 'czechia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '79e6c618-6bc9-f140-7346-3ff33d034c97', 'country', 'Côte d''Ivoire', 'cote d''ivoire', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f666b8e5-10a1-898f-5d0c-d6e36f689d15', 'country', 'Cộng hòa Xã hội chủ nghĩa Việt Nam', 'cong hoa xa hoi chu nghia viet nam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '47f782ca-9538-9833-0815-03be727fe9ac', 'country', 'DPRK', 'dprk', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b0546a57-4345-4722-8323-88b7c6402c18', 'country', 'DR Congo', 'dr congo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '10aa860d-f3cd-8974-4524-cf82f7d4e5cf', 'country', 'Danmark', 'danmark', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b295ec60-3d75-bde0-def2-3da0cc6052f7', 'country', 'Dawlat Iritriyá', 'dawlat iritriya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c29ca20d-c600-d090-fcf0-30001a4b7688', 'country', 'Dawlat Libya', 'dawlat libya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '76821d68-e8a3-b43e-d3b8-4d1442bc6a93', 'country', 'Dawlat Qaṭar', 'dawlat qatar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '05b85922-6dec-d266-b7ba-718a3a0b2366', 'country', 'Dawlat al-Kuwait', 'dawlat al-kuwait', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '54096d3a-f0a1-2418-2f9b-235863387f50', 'country', 'Democratic People''s Republic of', 'democratic people''s republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fcae9dbb-6e81-9c33-6078-a1b4be24e738', 'country', 'Democratic People''s Republic of Korea', 'democratic people''s republic of korea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c149f552-fbfd-56e2-9af5-8316a505437d', 'country', 'Democratic Republic of São Tomé and Príncipe', 'democratic republic of sao tome and principe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1e256255-942b-1364-3eb9-b840fade80df', 'country', 'Democratic Republic of Timor-Leste', 'democratic republic of timor-leste', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd1059f01-d0ef-d61d-bd26-51a18240f72f', 'country', 'Democratic Socialist Republic of Sri Lanka', 'democratic socialist republic of sri lanka', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5fe0d437-85f5-aa74-0fb4-1103beb7702d', 'country', 'Denmark', 'denmark', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c2a840fc-1d80-e29a-fa80-9ab5c14f8772', 'country', 'Dhivehi Raajjeyge Jumhooriyya', 'dhivehi raajjeyge jumhooriyya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '05ba3142-e9b0-3a35-4c14-4a0d0c02ffc1', 'country', 'Djibouti', 'djibouti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a2feed8e-da6e-2099-e5e7-3a6cb094886c', 'country', 'Dominica', 'dominica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '797d1ddc-2427-c189-a056-319dfc4e92d3', 'country', 'Dominican Republic', 'dominican republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '48b4516c-546c-af3c-7786-f795e9e08466', 'country', 'Dominique', 'dominique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'da3c09e9-e6ff-b887-f576-93d7a2a4c1b6', 'country', 'Dzayer', 'dzayer', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 100 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9df307d1-6b87-8f26-5fc2-b20fa95e58d1', 'country', 'East Timor', 'east timor', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9b97a2e3-83dc-ad13-bab9-ce1b77f0af2c', 'country', 'Ecuador', 'ecuador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '08cd84ee-7723-c3ab-7f80-044133f32517', 'country', 'Eesti', 'eesti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5525332b-4eef-b702-9099-eca9f4708d86', 'country', 'Eesti Vabariik', 'eesti vabariik', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '53c2f8c5-43b2-cead-c34d-c58dda714ef3', 'country', 'Egypt', 'egypt', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0b234a7e-dfd8-479d-9f20-77c62a151a1b', 'country', 'El Salvador', 'el salvador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fd02fdbf-3490-6eb0-80df-5f57c5caef14', 'country', 'Elláda', 'ellada', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a3590d6f-0641-9d7b-1f76-21c28c924464', 'country', 'Emirates', 'emirates', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5b2f42dd-f084-29b8-2c07-dfde05b2c5ba', 'country', 'Equatorial Guinea', 'equatorial guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c93fc1e3-32dd-58f5-6348-cd0cb27fcda9', 'country', 'Eritrea', 'eritrea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '391309fb-ee0c-27a0-fb88-ea570d7cc672', 'country', 'Estado Plurinacional de Bolivia', 'estado plurinacional de bolivia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1f96ea5f-255a-a3a5-8e2d-3fcb8d7ff285', 'country', 'Estados Unidos Mexicanos', 'estados unidos mexicanos', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '74199e83-46c5-abb6-f93a-b54e554d7d00', 'country', 'Estonia', 'estonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bf021143-55ff-895b-2d8f-ce7f34bbff0b', 'country', 'Eswatini', 'eswatini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2b5f6843-8cee-ce30-b869-9833adfaa8ea', 'country', 'Ethiopia', 'ethiopia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'daa2561c-5699-7a77-209a-e5c5133a821b', 'country', 'Federal Democratic Republic of Ethiopia', 'federal democratic republic of ethiopia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f292de9b-6d7f-be35-a9a1-e88a18d934f3', 'country', 'Federal Democratic Republic of Nepal', 'federal democratic republic of nepal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ce6d575a-c701-7a29-05ed-8420b067de8c', 'country', 'Federal Republic of Germany', 'federal republic of germany', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '102e6091-3b96-6ffe-e51a-9c89ef1173d5', 'country', 'Federal Republic of Nigeria', 'federal republic of nigeria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'de450cef-6fd9-43a6-f5ad-92809ffd3e13', 'country', 'Federal Republic of Somalia', 'federal republic of somalia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b36ce2bf-4c12-b4b8-76cf-700c2f64be8b', 'country', 'Federated States of', 'federated states of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '76dcfb84-e029-0854-81a1-bfa1217f6786', 'country', 'Federated States of Micronesia', 'federated states of micronesia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bc0f77fd-8266-7ad6-a540-22391df2de6c', 'country', 'Federation of Saint Christopher and Nevis', 'federation of saint christopher and nevis', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4e57c28d-c509-474f-af0e-5d2d73802cc2', 'country', 'Federative Republic of Brazil', 'federative republic of brazil', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '54333a90-d374-4173-9a42-6456afb92f32', 'country', 'Fiji', 'fiji', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7e5d1c11-3d18-72fa-4099-67dcd8f49b28', 'country', 'Fijī Gaṇarājya', 'fiji ganarajya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '675e6ac5-4eef-a859-d03e-517a98c5170f', 'country', 'Finland', 'finland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '604a2e30-2630-c572-0ed9-bd2b643276a5', 'country', 'France', 'france', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6586fe4d-3408-31b9-f5af-28da1678fe76', 'country', 'French Republic', 'french republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '87011f91-b37c-06c5-3650-7bad770a7c1c', 'country', 'Fürstentum Liechtenstein', 'furstentum liechtenstein', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '292a1474-88d0-3616-7e29-0f71240b6d0f', 'country', 'Gabon', 'gabon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8854142f-23e8-cdba-4232-7055d427dd04', 'country', 'Gabonese Republic', 'gabonese republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '19550f8e-220e-5c5c-ef5e-d1fb1c53cf7d', 'country', 'Gabuuti', 'gabuuti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9ab2976a-49f9-988f-9192-e0a492c94186', 'country', 'Gabuutih Ummuuno', 'gabuutih ummuuno', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6cc5df4d-8f1e-58e0-6207-91c9b959044e', 'country', 'Gambia', 'gambia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3182e62d-64d5-fc2a-4db1-975984b1357b', 'country', 'Georgia', 'georgia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7d0c0a4b-ef16-0f68-1373-be2e0253cf7c', 'country', 'Germany', 'germany', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b6a3d7f9-a09b-b83f-b212-5c9f75232e24', 'country', 'Ghana', 'ghana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a0f313ce-674b-ec6e-722a-90f0860ac30f', 'country', 'Grand Duchy of Luxembourg', 'grand duchy of luxembourg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e24b8dda-c0c7-06eb-e2a2-69792b44df45', 'country', 'Grand-Duché de Luxembourg', 'grand-duche de luxembourg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '211c2abb-747b-e77e-69be-88dd50d654a2', 'country', 'Great Britain', 'great britain', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6830747f-6a2f-70ed-47de-4761e8bdc599', 'country', 'Greece', 'greece', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '31b309ac-974e-9a44-54f6-5a1491d8419d', 'country', 'Grenada', 'grenada', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '95b71fbe-f0c3-2cb7-a300-4a640a9e59d4', 'country', 'Groussherzogtum Lëtzebuerg', 'groussherzogtum letzebuerg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b5b3dd24-4597-9d8f-6a96-9b68971f31cc', 'country', 'Großherzogtum Luxemburg', 'grossherzogtum luxemburg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '94e61edd-2fd8-0b9a-f7a5-fa280f24fd2f', 'country', 'Guatemala', 'guatemala', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '497051bb-493b-de51-aa5c-15bb8accd7e4', 'country', 'Guinea', 'guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5db047a5-0dcc-4eef-b5aa-7d2b024cf8e5', 'country', 'Guinea-Bissau', 'guinea-bissau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '74a3e197-874b-3dc3-22e0-e1bb51b20697', 'country', 'Guyana', 'guyana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c06e630b-2da3-112c-1d55-fc6837a913b5', 'country', 'Gônôprôjatôntri Bangladesh', 'gonoprojatontri bangladesh', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 150 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '715b1538-da68-c020-a332-f3869c08534c', 'country', 'Haiti', 'haiti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c1267bb7-a401-5092-7305-09f4d8c4d5b9', 'country', 'Hashemite Kingdom of Jordan', 'hashemite kingdom of jordan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1856d771-6add-8bb0-d150-18388d924c68', 'country', 'Hayastan', 'hayastan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a333e721-c56c-369a-aeaa-e559953139e8', 'country', 'Hellenic Republic', 'hellenic republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e3722b58-4b8b-2dc1-91a6-6fbcdd618401', 'country', 'Holland', 'holland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7b723a95-7e88-216a-c62b-cbbde091091c', 'country', 'Holy See (Vatican City State)', 'holy see (vatican city state)', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0023257a-ef85-8004-f5c2-3a32d584423c', 'country', 'Honduras', 'honduras', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '25f39768-8b2f-7ca2-c07b-290e19e3afb3', 'country', 'Hrvatska', 'hrvatska', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5c33d145-8924-0067-14d9-ff5ad2b882d8', 'country', 'Hungary', 'hungary', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c60eef78-7180-e59d-5d40-ed5e67f9e7ac', 'country', 'Iceland', 'iceland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8994c507-e81f-5039-3dbd-5d7be8e9e596', 'country', 'Independen Stet bilong Papua Niugini', 'independen stet bilong papua niugini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '70ff9eb3-a5d4-5753-eeeb-549649dbd95a', 'country', 'Independent State of Papua New Guinea', 'independent state of papua new guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '59ce2108-0cd7-6bc9-c8d8-3340bfbbdc7d', 'country', 'Independent State of Samoa', 'independent state of samoa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ba88ce3e-7ac4-ade5-5d5b-d6d5fa9c7c71', 'country', 'India', 'india', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6a2771db-8470-b9cd-54a6-7312dd58bb14', 'country', 'Indonesia', 'indonesia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '91005696-e3c8-5169-2897-ab56a98b7eb8', 'country', 'Iran', 'iran', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '35cf24a7-606b-052b-eff2-2dbefe359584', 'country', 'Iraq', 'iraq', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b496b5aa-1a1a-7a5c-f3cb-6a0929e7e79f', 'country', 'Ireland', 'ireland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '11bf7d3a-3eae-f1c4-780e-7491e0ff64cc', 'country', 'Iritriyā', 'iritriya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '66fa90ce-c505-0e5d-ce9b-b74fd5216416', 'country', 'Islamic Republic of', 'islamic republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd78b3219-a829-1959-8e75-47ceec3535d5', 'country', 'Islamic Republic of Iran', 'islamic republic of iran', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8307bf28-d500-5687-c700-e20c04955f5d', 'country', 'Islamic Republic of Mauritania', 'islamic republic of mauritania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f79cbec4-f152-8cb6-5a04-d943cdd52ffa', 'country', 'Islamic Republic of Pakistan', 'islamic republic of pakistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '854b7074-66cb-bfbb-1cb0-1adf4de8f346', 'country', 'Island', 'island', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c5fb7c69-4601-0053-401c-ca535e862b79', 'country', 'Islāmī Jumhūriya''eh Pākistān', 'islami jumhuriya''eh pakistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '92e37341-179b-ff81-c160-df145791bf12', 'country', 'Israel', 'israel', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a3ad889a-ff21-bfd4-d8f9-b2565bbc8373', 'country', 'Italian Republic', 'italian republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7665913d-11e7-63ae-bd02-29fb82329b21', 'country', 'Italy', 'italy', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c4201872-060d-08d0-a482-2a657159c0de', 'country', 'Ivory Coast', 'ivory coast', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '733f56a0-5d52-ea1c-2765-8767d117a8d9', 'country', 'Jabuuti', 'jabuuti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '434e0c97-9da5-d4cf-8e6f-f58eb2c91002', 'country', 'Jamaica', 'jamaica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9f8e8ecd-749d-e9f0-864d-825c42df1e54', 'country', 'Jamhuri ya Kenya', 'jamhuri ya kenya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'edc6cf0e-7a09-2aeb-79fd-0a1f815a56a2', 'country', 'Jamhuri ya Muungano wa Tanzania', 'jamhuri ya muungano wa tanzania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6d20d0f9-4b3c-af03-2773-bb40e9bda556', 'country', 'Jamhuri ya Uganda', 'jamhuri ya uganda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1f4dc570-4ffb-440f-8b3b-f119db16a06f', 'country', 'Jamhuuriyadda Federaalka Soomaaliya', 'jamhuuriyadda federaalka soomaaliya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c9a2e41-8ae7-34d9-fecb-f4a3e0916a4d', 'country', 'Jamhuuriyadda Jabuuti', 'jamhuuriyadda jabuuti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8a59ea4e-3cb6-8243-e7e1-780de108fdee', 'country', 'Japan', 'japan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '643b0e4c-102c-3dd2-690e-269274e5d464', 'country', 'Jomhuri-ye Eslāmi-ye Irān', 'jomhuri-ye eslami-ye iran', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a552ba51-8cfa-0962-eadc-affaea49df40', 'country', 'Jordan', 'jordan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1e5e03bf-2c50-4469-6fbf-eb377af87785', 'country', 'Jumhūriyyat al-‘Irāq', 'jumhuriyyat al-‘iraq', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '13e647e6-02ff-8274-bcc8-cbe1fd70ba59', 'country', 'Jumhūriyyat aṣ-Ṣūmāl al-Fiderāliyya', 'jumhuriyyat as-sumal al-fideraliyya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '50ade04d-4b44-026d-4b2c-e470ad587fe0', 'country', 'Jumhūrīyat as-Sūdān', 'jumhuriyat as-sudan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '91492d19-2a0c-18bf-1a65-1577ab840e20', 'country', 'Kazakhstan', 'kazakhstan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c8078a6a-c641-0aa3-9067-abd651de12af', 'country', 'Kenya', 'kenya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6fdc3b9c-74b4-268c-d21d-0309eae98fa3', 'country', 'Kingdom of Bahrain', 'kingdom of bahrain', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fb141f90-7286-b1f4-f3ca-8720e7d8a172', 'country', 'Kingdom of Belgium', 'kingdom of belgium', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2f9dc2c4-3e29-c3b4-b03b-40ac918acd11', 'country', 'Kingdom of Bhutan', 'kingdom of bhutan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2dbb8194-702e-08fa-b852-e02abae78b21', 'country', 'Kingdom of Cambodia', 'kingdom of cambodia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '303f18df-e959-a9fd-6971-5ba27260ce83', 'country', 'Kingdom of Denmark', 'kingdom of denmark', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2ee420d1-b672-c556-043f-fb31633f8d6e', 'country', 'Kingdom of Eswatini', 'kingdom of eswatini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 200 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '58efad72-dc96-0f6a-0db5-168a1a6f7b99', 'country', 'Kingdom of Lesotho', 'kingdom of lesotho', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0aa30aab-3137-8823-c67e-0f2e6b082662', 'country', 'Kingdom of Morocco', 'kingdom of morocco', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '70b1c74c-a50f-0852-e9b7-1f103e6d7db5', 'country', 'Kingdom of Norway', 'kingdom of norway', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd4a95a57-b017-1e14-0b69-e8b143b1b805', 'country', 'Kingdom of Saudi Arabia', 'kingdom of saudi arabia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7b83b188-5c6a-6e12-8e8d-08d5e269e59f', 'country', 'Kingdom of Spain', 'kingdom of spain', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3fd1c39a-3245-add3-344e-bc6fbe938ef2', 'country', 'Kingdom of Sweden', 'kingdom of sweden', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b152932d-4824-8a00-1306-c843bb377d6e', 'country', 'Kingdom of Thailand', 'kingdom of thailand', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8da31af3-9c80-bb91-088f-9a47b7e059d9', 'country', 'Kiribati', 'kiribati', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '198076b9-24f8-a24b-99da-d1238c62b809', 'country', 'Kongeriget Danmark', 'kongeriget danmark', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a26c7d72-fdc2-e3dc-a87f-c4068473be4c', 'country', 'Kongeriket Noreg', 'kongeriket noreg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e0291391-f366-918e-4b61-6b2bf91550cc', 'country', 'Kongeriket Norge', 'kongeriket norge', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'eddc77ba-2265-80c0-782f-aa1968827d45', 'country', 'Koninkrijk België', 'koninkrijk belgie', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '223a5c2f-72fb-4208-1867-99d974d03307', 'country', 'Konungariket Sverige', 'konungariket sverige', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '879af3fe-3c29-b087-549c-c522afa16c29', 'country', 'Korea', 'korea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '55255b3d-b129-5373-b242-76f72e1442a1', 'country', 'Kosovo', 'kosovo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '14c8dd98-23d0-0195-1432-ba1405547bd2', 'country', 'Kuwait', 'kuwait', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '603a70bc-ac40-b3ec-4d1e-9244266d1bcd', 'country', 'Kyrgyz Republic', 'kyrgyz republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e8f01797-632c-c559-3e1f-d71ffd7b2553', 'country', 'Kyrgyz Respublikasy', 'kyrgyz respublikasy', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b1a0709f-1760-5bcd-fd3b-26bec895c827', 'country', 'Kyrgyzstan', 'kyrgyzstan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f1cc1fb9-1bc8-96e9-0ad9-7d94ffeb4f5e', 'country', 'Königreich Belgien', 'konigreich belgien', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '325d5478-7a5e-751b-c870-e1e07e388c48', 'country', 'Kýpros', 'kypros', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0fe6824b-981e-f5cc-ba20-fbd7de86a5a4', 'country', 'Kıbrıs', 'kıbrıs', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9ef41f99-d75f-3f11-9bde-0db28b343019', 'country', 'Kıbrıs Cumhuriyeti', 'kıbrıs cumhuriyeti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '12ef5836-ca4f-7bee-5c39-778b700fc77d', 'country', 'Lao', 'lao', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c2e442fe-f285-1ab3-664a-e82ba2a4747e', 'country', 'Lao People''s Democratic Republic', 'lao people''s democratic republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3fee92fe-a3a0-b66a-5250-046dbe3a7755', 'country', 'Laos', 'laos', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fd1d0a45-f4c8-3a08-f1ac-72e3d38b4c23', 'country', 'Latvia', 'latvia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '29170e1e-d013-1bfa-d952-e185dbeda3ae', 'country', 'Latvijas Republika', 'latvijas republika', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ebf6cab8-2187-fcbc-c2b7-812b4ce01f0a', 'country', 'Lebanese Republic', 'lebanese republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd0482d71-aadb-5dec-1651-2adfd90ab629', 'country', 'Lebanon', 'lebanon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '548bfe0e-eb27-c752-94b4-35d28ef7d039', 'country', 'Lefatshe la Botswana', 'lefatshe la botswana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e35ab424-5180-81b7-5c3b-67ca8d0a6f9c', 'country', 'Lesotho', 'lesotho', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3ccdf02a-b131-f0be-7e8d-3825193eb0cf', 'country', 'Liberia', 'liberia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '22916422-e842-9e1d-d5be-351ea231976e', 'country', 'Libya', 'libya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ff4a5465-2a10-c048-b777-28e31393643e', 'country', 'Liechtenstein', 'liechtenstein', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f99d5063-801b-ac49-fb3d-f050e96ce793', 'country', 'Lietuvos Respublika', 'lietuvos respublika', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fb677bec-a8c4-2629-6768-4a65e450d11e', 'country', 'Lithuania', 'lithuania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6b90ef6e-b212-6619-a03a-22ac9ab6500f', 'country', 'Loktāntrik Ganatantra Nepāl', 'loktantrik ganatantra nepal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6dd6a422-a472-d8fc-5b7d-9c4bf58a8100', 'country', 'Luxembourg', 'luxembourg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '03beeb0f-0278-c2ce-4009-74cb69f32f5c', 'country', 'Lýðveldið Ísland', 'lydveldid island', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c67c4580-20f1-2c8d-9ccd-ccbc0d332255', 'country', 'Macedonia', 'macedonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ef446b9d-4ba0-7d7b-3a1f-2ddfe1809e14', 'country', 'Madagascar', 'madagascar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5ea198ca-88f8-f370-cd20-62f4ed3568b4', 'country', 'Malawi', 'malawi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a76bc7d2-bef7-6e79-ba83-755daac3b154', 'country', 'Malaysia', 'malaysia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b74bca49-0ae0-b814-9137-b3d0753e977b', 'country', 'Maldive Islands', 'maldive islands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '20963fc8-c57a-3aa4-01d9-93a06baf69d7', 'country', 'Maldives', 'maldives', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a04fd2ee-9497-6672-f7f8-6e28e2bb5c1a', 'country', 'Mali', 'mali', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2f9747de-db05-5880-52b1-a0bf3f051421', 'country', 'Malo Saʻoloto Tutoʻatasi o Sāmoa', 'malo saʻoloto tutoʻatasi o samoa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '677898bd-e598-f9fd-cbcc-4e82b078cbf1', 'country', 'Malta', 'malta', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '80a980d3-ac73-dded-6035-4e1570713416', 'country', 'Mamlakat al-Baḥrayn', 'mamlakat al-bahrayn', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 250 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '74a693cd-d949-3b4c-8fe5-f9a6251fcd9f', 'country', 'Marshall Islands', 'marshall islands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a846803a-4ff4-f592-ccd1-ba1f433d5ed2', 'country', 'Matanitu ko Viti', 'matanitu ko viti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '03f72f8e-266e-cd36-5e9c-7eb394c61962', 'country', 'Mauritania', 'mauritania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c44ab03c-6680-aa5e-9949-308e2d2dd41d', 'country', 'Mauritius', 'mauritius', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5fb090d4-0677-eab6-8dd1-873cd717f055', 'country', 'Medīnat Yisrā''el', 'medinat yisra''el', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1894cbaf-2157-2f70-899f-58b49d498b72', 'country', 'Mexicanos', 'mexicanos', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fed82d30-1281-0a96-88f7-c07e2e289c00', 'country', 'Mexico', 'mexico', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e19a1392-08da-dea9-6531-78159827cce3', 'country', 'Micronesia', 'micronesia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4b1a953a-78b7-8750-ef10-6b44d40ca01c', 'country', 'Moldova', 'moldova', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '13e6e877-4d14-cb35-6950-cdb91818d0be', 'country', 'Monaco', 'monaco', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '565a0602-ebf6-053d-0bae-3035748441a9', 'country', 'Mongolia', 'mongolia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '075f2ffd-4969-5853-427e-4b9d2bb2c792', 'country', 'Montenegro', 'montenegro', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3e9e4711-67f9-94dd-dc64-61edd78085c6', 'country', 'Morocco', 'morocco', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '874ac48c-d8c1-05af-f4e4-efab191f8d4c', 'country', 'Mozambique', 'mozambique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '47e62129-6665-c2fb-e808-9bcd1ec4e314', 'country', 'Muso oa Lesotho', 'muso oa lesotho', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'affc3ca6-c5d0-855e-7d36-b1e8a720086e', 'country', 'Myanmar', 'myanmar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a2f0d835-cd05-839f-69e7-35cb855941f7', 'country', 'Namibia', 'namibia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '22abc73a-8a14-4d32-339c-b8b85b1f526b', 'country', 'Namibië', 'namibie', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '40cb7b51-bc80-6a30-4bb0-b7e49dec6a1f', 'country', 'Naoero', 'naoero', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5f76df76-f5a8-a285-5536-dfd298c2237e', 'country', 'Nation of Brunei', 'nation of brunei', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9e4340d4-7c5d-7439-649e-a48f65ae3bbc', 'country', 'Nauru', 'nauru', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f18124bc-7c7f-2992-5689-713d7b4d01a9', 'country', 'Naíjíríà', 'naijiria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '98f7338f-7b9f-c7e9-6cec-6bce6b3c31cd', 'country', 'Nederland', 'nederland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e7701f93-197c-1abe-de39-473333de1c8a', 'country', 'Nepal', 'nepal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c31806a-a99e-69e7-7232-f2338cd869e8', 'country', 'Netherlands', 'netherlands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '75aa9960-2dc9-fbbc-0e2f-c8359e64117d', 'country', 'New Zealand', 'new zealand', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c580692d-169f-5712-2313-d204babc826c', 'country', 'Ngwane', 'ngwane', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '907e536e-e058-789e-c0e2-d205fca6a8b2', 'country', 'Nicaragua', 'nicaragua', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a01b56ac-cf03-758e-15a0-8bc41215572a', 'country', 'Niger', 'niger', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c06bc776-0be8-bbaf-ffc8-9a6024fd68bb', 'country', 'Nigeria', 'nigeria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '556ae758-fc52-245a-8ca4-0e441558aafc', 'country', 'Nihon', 'nihon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '711fca9d-b743-ec3d-bd4f-92c0952ec6eb', 'country', 'Nijar', 'nijar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '722e3807-f630-0494-cce5-638dac19cffb', 'country', 'Nijeriya', 'nijeriya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8ad6afcb-9844-314b-3630-48a99c7fed80', 'country', 'Nippon', 'nippon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e45c2ced-10bd-3b30-61f7-f0f81949631f', 'country', 'Noreg', 'noreg', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '160961e7-4612-a7c5-7122-d0d7532e79b2', 'country', 'Norge', 'norge', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e0127003-d80e-d360-4a02-71afbbe2a37b', 'country', 'North Korea', 'north korea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4d16ddfa-9313-7e21-791d-86311a0b82a8', 'country', 'North Macedonia', 'north macedonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fe916705-67cc-1a47-3ac3-e802278ba936', 'country', 'Norway', 'norway', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '48b7ef07-63ca-0da4-0170-b29b2f14d460', 'country', 'Oesterreich', 'oesterreich', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '06dbaef1-f389-971a-dd23-d5718e1005fa', 'country', 'Oman', 'oman', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '097a9448-4764-3dba-1c98-fe0729322983', 'country', 'Oriental Republic of Uruguay', 'oriental republic of uruguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '49b96a80-6480-4e4d-1274-8a0b6b4ff5bc', 'country', 'Osterreich', 'osterreich', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6accbd91-af5d-775e-b390-cb94f340ce4c', 'country', 'O‘zbekiston Respublikasi', 'o‘zbekiston respublikasi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '94cbeed9-7573-d5b2-7312-89f4b6b0c01a', 'country', 'Pakistan', 'pakistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '607d6f47-43ee-8b4c-cda2-de0361ebb532', 'country', 'Palau', 'palau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'aeece0d5-643d-7a1e-4a1b-df6b8b5f000b', 'country', 'Panama', 'panama', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b77b82aa-a93e-c669-7c90-e0007f50d3f9', 'country', 'Papua New Guinea', 'papua new guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4122be14-c0f6-3b1f-aa7f-cb9d60d4e04d', 'country', 'Paraguay', 'paraguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '75ad2327-4ca7-cb52-5f78-72fa8add7467', 'country', 'People''s Republic of Bangladesh', 'people''s republic of bangladesh', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 300 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f7bb813f-2341-41dc-dca2-702b5b18c322', 'country', 'People''s Republic of China', 'people''s republic of china', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a32616c9-20bf-b6df-4845-dc879e2ef94f', 'country', 'Peru', 'peru', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '35b2256c-549b-bbc0-b63b-92f07c07461e', 'country', 'Philippines', 'philippines', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a8d0e495-68e9-c478-cf7c-1193562bc4e1', 'country', 'Pleasant Island', 'pleasant island', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '28a1df04-f1bf-cbc4-07cc-444977b4a1f9', 'country', 'Plurinational State of', 'plurinational state of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '82dcc144-df36-425f-3168-2886f650ba8a', 'country', 'Plurinational State of Bolivia', 'plurinational state of bolivia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4626cef5-2e2e-e1af-152e-ea06bbd70312', 'country', 'Poblacht na hÉireann', 'poblacht na heireann', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ffe6f166-1b55-aa94-3704-0f062c9981a2', 'country', 'Poland', 'poland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b99cd8d4-7e67-fe8c-68fc-d5aeaad0709a', 'country', 'Portugal', 'portugal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '077ff6d1-5373-fbba-919c-d55f206884a7', 'country', 'Portuguesa', 'portuguesa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f894ef77-7530-8357-cb9b-8f95b8d4f50e', 'country', 'Portuguese Republic', 'portuguese republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '260ec47e-f6f0-025d-db2e-0f3f8e0018f1', 'country', 'Prathet', 'prathet', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a335db30-4355-c070-f320-23371950b9c5', 'country', 'Principality of Andorra', 'principality of andorra', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '19eb933e-1650-d7ce-1cdb-d9eb46591652', 'country', 'Principality of Liechtenstein', 'principality of liechtenstein', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4d7322f1-87e3-95be-7bed-155c0fdecd32', 'country', 'Principality of Monaco', 'principality of monaco', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f7a9eba5-b8b3-e542-838e-6533dbdba072', 'country', 'Principat d''Andorra', 'principat d''andorra', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c3545842-9d3d-4a62-5a1e-b022b25478b3', 'country', 'Principauté de Monaco', 'principaute de monaco', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '21a3b77b-dff9-58b0-122d-262c07a63278', 'country', 'Pyidaunzu Thanmăda Myăma Nainngandaw', 'pyidaunzu thanmada myama nainngandaw', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e9c7cdac-e1f9-86bc-14b0-2c89b20afd85', 'country', 'Pākistān', 'pakistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e021d48f-174f-6410-dc58-79ad701c0417', 'country', 'Qatar', 'qatar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd6c17f5f-5ce8-d059-9e04-15448c5b29f1', 'country', 'Qazaqstan', 'qazaqstan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cb35f8e8-362d-5a61-d184-d16f7f3501e7', 'country', 'Qazaqstan Respublïkası', 'qazaqstan respublikası', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9efb36e2-70d5-1ba8-93cb-9de68c541727', 'country', 'Ratcha Anachak Thai', 'ratcha anachak thai', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'dac0bd3d-fd5a-b391-e930-9d440185e96f', 'country', 'Reino de España', 'reino de espana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2443bfa8-3636-1239-bc1e-10b0ec415455', 'country', 'Repiblik Ayiti', 'repiblik ayiti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9dac6529-5f85-0ff7-f72f-853427b5021b', 'country', 'Repiblik Sesel', 'repiblik sesel', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '07fd7013-6212-3d47-965c-c8448cab368e', 'country', 'Repoblikan''i Madagasikara', 'repoblikan''i madagasikara', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '119cb9ae-af9e-226c-91c0-8158f26a0c0e', 'country', 'Repubblica di San Marino', 'repubblica di san marino', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7729b637-a960-8215-8d46-deb87ada094b', 'country', 'Repubblica italiana', 'repubblica italiana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8e6d5cc2-edef-6463-bac3-1bede785e380', 'country', 'Repubblika ta'' Malta', 'repubblika ta'' malta', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '29e413e7-5087-b92d-0fe5-9e23aad5a1d9', 'country', 'Republic of', 'republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bdc10610-16ed-c3ea-8fb9-9483b00b5ac9', 'country', 'Republic of Armenia', 'republic of armenia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '229dbcc6-bbb1-22cd-dd30-c294741feacf', 'country', 'Republic of Azerbaijan', 'republic of azerbaijan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '68204707-48ae-4c71-d396-aa62d7daa8c0', 'country', 'Republic of Belarus', 'republic of belarus', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8fea45d4-8b29-fe69-088d-5db204040a59', 'country', 'Republic of Benin', 'republic of benin', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ae096ca0-416d-199a-e5d5-1db8bc4773aa', 'country', 'Republic of Botswana', 'republic of botswana', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '01f1f446-b4f1-5385-e7b1-add7b11a5667', 'country', 'Republic of Bulgaria', 'republic of bulgaria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e53a4018-1b97-413b-97e3-ea40027dd871', 'country', 'Republic of Burundi', 'republic of burundi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'dd7b840b-9eae-5347-8ebd-dde3a44621de', 'country', 'Republic of Cabo Verde', 'republic of cabo verde', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e1cb8c3d-0cff-5d6d-8486-d0cb6012f48c', 'country', 'Republic of Cameroon', 'republic of cameroon', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2a3d4af7-641b-e528-6531-6a362e1cab4a', 'country', 'Republic of Chad', 'republic of chad', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '238b10dc-20b1-f56f-2d98-a41889732b55', 'country', 'Republic of Chile', 'republic of chile', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f1574171-24d9-dbad-4951-8d8bd1932837', 'country', 'Republic of Colombia', 'republic of colombia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '797a9955-9141-ab8d-42df-66acfaa1207a', 'country', 'Republic of Costa Rica', 'republic of costa rica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '262e5d60-5cc6-1f52-09ed-57852ad6b384', 'country', 'Republic of Croatia', 'republic of croatia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a0823a4b-56e0-96d9-f1dc-8a265b29ae1f', 'country', 'Republic of Cuba', 'republic of cuba', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cf2b14b8-4f81-32e3-94f1-86743739ff7c', 'country', 'Republic of Cyprus', 'republic of cyprus', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '95fb2a19-859d-ca25-98ab-00aec2857cb7', 'country', 'Republic of Côte d''Ivoire', 'republic of cote d''ivoire', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0c1e63f1-d0f1-7a04-a20f-67fc3c6900a8', 'country', 'Republic of Djibouti', 'republic of djibouti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f5354265-5b16-05f9-0dd3-58c6d898ecd7', 'country', 'Republic of Ecuador', 'republic of ecuador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 350 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2397498e-0547-5e20-f967-93ba0509ed6f', 'country', 'Republic of El Salvador', 'republic of el salvador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '85dcd736-e019-e3bd-8bef-684df858130b', 'country', 'Republic of Equatorial Guinea', 'republic of equatorial guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2faa9fbc-8422-3afa-5d45-349d63e287c6', 'country', 'Republic of Estonia', 'republic of estonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a2c9684c-6592-fe6a-dd9b-d066bbef95f7', 'country', 'Republic of Fiji', 'republic of fiji', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9554563e-ff85-3c68-0a3b-4a6bd62c093c', 'country', 'Republic of Finland', 'republic of finland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8c72256c-c7a2-c425-dfe7-ffb58e4a9f65', 'country', 'Republic of Guinea', 'republic of guinea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f503e83c-ec05-6e33-4632-1cb46978925f', 'country', 'Republic of Guinea-Bissau', 'republic of guinea-bissau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5e84a7f3-1b81-caf3-c668-0d471eead79a', 'country', 'Republic of Haiti', 'republic of haiti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cbd03d0d-0042-1427-6137-180938f8e3dd', 'country', 'Republic of Honduras', 'republic of honduras', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e111e11d-9057-7098-e30a-61ead9d847bf', 'country', 'Republic of Iceland', 'republic of iceland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '841d2087-fe9d-4b06-50e9-b0c9e891b348', 'country', 'Republic of India', 'republic of india', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9fd55a1f-3519-a506-db48-3dba44324d9d', 'country', 'Republic of Indonesia', 'republic of indonesia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '45451c59-6f8a-f458-b889-ea190bece6b4', 'country', 'Republic of Iraq', 'republic of iraq', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f183edb9-2519-d757-76d2-5f72853aa04b', 'country', 'Republic of Ireland', 'republic of ireland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0b9f0694-b0f0-0a2f-9c29-18dce1bf9846', 'country', 'Republic of Kazakhstan', 'republic of kazakhstan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '08aa4df6-c835-b265-0d02-0083afb17050', 'country', 'Republic of Kenya', 'republic of kenya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ad0e72d4-8a45-6c17-9d02-3bf1304e7d3f', 'country', 'Republic of Kiribati', 'republic of kiribati', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '05cb7b77-0e43-554e-ec34-49bca0033474', 'country', 'Republic of Korea', 'republic of korea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'eb0a018c-f6f7-09be-22cd-f68646ff6fa8', 'country', 'Republic of Latvia', 'republic of latvia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '18e489ee-3789-4c39-86bf-21b2aa2e9981', 'country', 'Republic of Liberia', 'republic of liberia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '844e4e2a-430a-53f3-dd10-2b7830076853', 'country', 'Republic of Lithuania', 'republic of lithuania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0943300f-764e-9a06-b114-50d934afd3ef', 'country', 'Republic of Madagascar', 'republic of madagascar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '601afff8-0e90-3d15-7725-99edc253a4a3', 'country', 'Republic of Malawi', 'republic of malawi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4c9d6eee-b440-14c8-5fb7-76658aaa0a48', 'country', 'Republic of Mali', 'republic of mali', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '05f7b673-ff08-f9c9-ce5c-5c0d9b21c647', 'country', 'Republic of Malta', 'republic of malta', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b8aa3513-c41b-eaaf-062d-6b515f3a8fea', 'country', 'Republic of Mauritius', 'republic of mauritius', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4122b1f6-3a69-fa57-82a9-f31b0f685818', 'country', 'Republic of Moldova', 'republic of moldova', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd72dcef8-f83b-9ace-f555-d443724bb36a', 'country', 'Republic of Mozambique', 'republic of mozambique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9361d471-d3cf-5428-d82a-186a808fc306', 'country', 'Republic of Namibia', 'republic of namibia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0ec65984-7b4e-75ac-68b7-11522199bf8a', 'country', 'Republic of Nauru', 'republic of nauru', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '08ca8440-07db-9d15-2fc8-a3b77494c11b', 'country', 'Republic of Nicaragua', 'republic of nicaragua', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4d0196b2-1b7b-2a3f-4182-0762a54db6f3', 'country', 'Republic of North Macedonia', 'republic of north macedonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd5e95d8f-aa73-5bb0-2df0-25ecbeec1935', 'country', 'Republic of Palau', 'republic of palau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6841f677-473d-3643-05ab-286ca52754bb', 'country', 'Republic of Panama', 'republic of panama', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4f0cff70-4882-06b6-2e31-e181ffa714dd', 'country', 'Republic of Paraguay', 'republic of paraguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '80905f3e-aeaa-213a-6e6b-e52d5bfc2dc4', 'country', 'Republic of Peru', 'republic of peru', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b8c8bf46-a7b7-5a7a-b237-e4bfb6ee5df2', 'country', 'Republic of Poland', 'republic of poland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ec1349bc-6ab2-7cf8-77f8-98cf3a6c6b98', 'country', 'Republic of Rwanda', 'republic of rwanda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd4e2f8ce-9fc0-5fb3-aec2-9bb8c08620f2', 'country', 'Republic of San Marino', 'republic of san marino', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a93eca9e-a762-b9b5-f39e-b6e588c3919e', 'country', 'Republic of Senegal', 'republic of senegal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1e4936f4-0d1f-49bd-126f-b2e0d384fb9d', 'country', 'Republic of Serbia', 'republic of serbia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b734d6e1-4b5e-06ce-48c9-3e3c7804800c', 'country', 'Republic of Seychelles', 'republic of seychelles', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0f1a3eec-233b-c2e3-47bb-9487d0f539ae', 'country', 'Republic of Sierra Leone', 'republic of sierra leone', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '68a1d963-5e70-b5da-bb1d-649be4d53914', 'country', 'Republic of Slovenia', 'republic of slovenia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2e83b34f-bf22-dc12-2f46-be1a322f7ea1', 'country', 'Republic of South Africa', 'republic of south africa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bac5de95-919e-ce01-b482-1b96dfc6ae65', 'country', 'Republic of Suriname', 'republic of suriname', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1927d55e-b206-0ecc-5d6c-7a3744ecefc5', 'country', 'Republic of Tajikistan', 'republic of tajikistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'caf12d37-2db4-a9f8-5b45-70f9971e9580', 'country', 'Republic of Trinidad and Tobago', 'republic of trinidad and tobago', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fe68f14e-6b97-299c-05be-91ad61882885', 'country', 'Republic of Tunisia', 'republic of tunisia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '08456547-02b7-d91e-75d4-49a4913bb9df', 'country', 'Republic of Turkey', 'republic of turkey', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 400 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c620c4d2-b6c0-4363-bd80-94515f457fef', 'country', 'Republic of Uganda', 'republic of uganda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8aff7d95-12cb-ca87-9ee4-f3f520d5875e', 'country', 'Republic of Uzbekistan', 'republic of uzbekistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '805f5fdf-17a7-bc70-a805-4e55cac7a542', 'country', 'Republic of Vanuatu', 'republic of vanuatu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0eda4a99-5176-2108-a66f-3f9139afe83c', 'country', 'Republic of Zambia', 'republic of zambia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b06123fe-e344-eb94-c4de-12290fef721c', 'country', 'Republic of Zimbabwe', 'republic of zimbabwe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8b83f323-bf73-876f-3833-615fd6924a27', 'country', 'Republic of the Congo', 'republic of the congo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '820b6ac8-e968-fb96-ef07-16e7c3b92be0', 'country', 'Republic of the Gambia', 'republic of the gambia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '872d7258-4359-79be-a74c-d49fb2239927', 'country', 'Republic of the Maldives', 'republic of the maldives', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a2a3482a-d0f6-a03f-16c5-8cf9620b3ecd', 'country', 'Republic of the Marshall Islands', 'republic of the marshall islands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c9830675-cc0a-2dbb-d070-95f5c9bdc81d', 'country', 'Republic of the Philippines', 'republic of the philippines', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e972c5d7-33b5-a7a2-c273-5738d2a58bae', 'country', 'Republic of the Sudan', 'republic of the sudan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '68c119e6-1efd-d499-a2ef-eab95bf25e15', 'country', 'Republic of the Union of Myanmar', 'republic of the union of myanmar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '311433ba-e9d6-7bef-a7b1-f4d5d32e68b7', 'country', 'Republica Moldova', 'republica moldova', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8ac281bb-2049-af12-13d4-79271a61d83b', 'country', 'Republiek Suriname', 'republiek suriname', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4af917d2-6379-be30-4387-2b8f8646d247', 'country', 'Republik Indonesia', 'republik indonesia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e8c7b3d6-46ee-5147-fd2c-2f357b84c3a5', 'country', 'Republik Singapura', 'republik singapura', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd28c3bf2-7b84-e86d-a7e8-480542288553', 'country', 'Republika Hrvatska', 'republika hrvatska', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '87e551ee-3a8d-ce2b-604c-3c0127d6874c', 'country', 'Republika Slovenija', 'republika slovenija', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8ddf0a95-b13f-1dc9-a7a1-62af22556934', 'country', 'Republika Srbija', 'republika srbija', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1f0ad927-bbeb-a6c6-596c-6d9d504eda6a', 'country', 'Republika y''Uburundi', 'republika y''uburundi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '48da7fb8-44c8-f84a-7ef3-cdb0b8e5a3da', 'country', 'Republiken Finland', 'republiken finland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '356ac624-db0e-e2b0-6ad0-c72fe4c8eaad', 'country', 'Repubulika y''u Rwanda', 'repubulika y''u rwanda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '75493046-d47b-4662-9095-456d84a33a7e', 'country', 'República Argentina', 'republica argentina', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ca5331f7-d655-b719-1040-6127641a6721', 'country', 'República Bolivariana de Venezuela', 'republica bolivariana de venezuela', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9fb317fd-0963-2c70-c1a5-8493e89d2819', 'country', 'República Democrática de São Tomé e Príncipe', 'republica democratica de sao tome e principe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bb4fc302-f6b8-7b24-ef3b-92843921063f', 'country', 'República Democrática de Timor-Leste', 'republica democratica de timor-leste', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f968b4e1-294d-1abd-7ec5-da93cb0e382c', 'country', 'República Federativa do Brasil', 'republica federativa do brasil', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '35c2963d-f18b-f864-30b1-0db1cb1473ef', 'country', 'República Oriental del Uruguay', 'republica oriental del uruguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a35ca9f7-6876-b545-dedb-8a72c9678674', 'country', 'República Portuguesa', 'republica portuguesa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4010f877-fbd2-0cd5-e9dd-19ca920e6555', 'country', 'República da Guiné Equatorial', 'republica da guine equatorial', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ebd20420-27a2-ee54-283f-b3ca515a2d14', 'country', 'República da Guiné-Bissau', 'republica da guine-bissau', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e5d5beb8-ae51-7827-90ba-d6af53ef94d8', 'country', 'República de Angola', 'republica de angola', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fc46d5ab-6b2b-5e69-6997-5620278848c7', 'country', 'República de Cabo Verde', 'republica de cabo verde', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd27637c7-5ddc-a8f1-d062-559805cd3810', 'country', 'República de Chile', 'republica de chile', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6595b7f6-51ad-02f9-9ba2-f89836861a1b', 'country', 'República de Colombia', 'republica de colombia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b44deef4-0324-a3d9-d5b2-5f4877b98716', 'country', 'República de Costa Rica', 'republica de costa rica', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '849807fc-810e-406a-cea2-2a779d09b10b', 'country', 'República de Cuba', 'republica de cuba', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b8738d30-496e-c5b7-8f6f-e81d9f6f73c6', 'country', 'República de El Salvador', 'republica de el salvador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '35535290-cff2-8a9f-c5ca-0ec7c846c221', 'country', 'República de Guinea Ecuatorial', 'republica de guinea ecuatorial', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd34151fb-b055-300c-a92e-a601f42b8188', 'country', 'República de Honduras', 'republica de honduras', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6ceea364-29d0-0677-48d7-85ec5eeada22', 'country', 'República de Moçambique', 'republica de mocambique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f9ea27d2-95f6-5c93-a988-fb4f4958f29a', 'country', 'República de Nicaragua', 'republica de nicaragua', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0da5ddd5-2365-052c-b5fe-e38bccdb6ee9', 'country', 'República de Panamá', 'republica de panama', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '83dcab81-5e20-7927-1ecb-707ad960fac6', 'country', 'República del Ecuador', 'republica del ecuador', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '65ffe2be-00c1-aa52-aea7-86d6402731b2', 'country', 'República del Paraguay', 'republica del paraguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5a9a9f10-83c4-466d-5ce6-3223b996f6ca', 'country', 'República del Perú', 'republica del peru', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'bc6ee98f-31ec-5df7-2f87-cf6172e4d112', 'country', 'Repúblika Demokrátika Timór-Leste', 'republika demokratika timor-leste', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4bbf25fc-9e08-ca00-815e-a765438b33f8', 'country', 'Repúblika ng Pilipinas', 'republika ng pilipinas', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '36f5cca1-4bea-9873-f77a-75cfab1face7', 'country', 'Respublika Kazakhstan', 'respublika kazakhstan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b0a04926-7cc1-b6c9-a59e-c1d4e66a89b0', 'country', 'Ribaberiki Kiribati', 'ribaberiki kiribati', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 450 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '087ae353-d474-63ce-3ed0-4ba8cb5c045b', 'country', 'Ripablik blong Vanuatu', 'ripablik blong vanuatu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5419b39d-a4f7-4796-1af1-152be68c1296', 'country', 'Ripublik Naoero', 'ripublik naoero', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c35f8c56-bdf9-1315-22ed-5b672049f2ee', 'country', 'Romania', 'romania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '035b525d-80e4-c4f0-ac79-91ce05a220cc', 'country', 'România', 'romania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0b301b5a-3bb1-fc6f-8360-2318918d7110', 'country', 'Roumania', 'roumania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a32c728f-77ed-97ce-73e3-f685fe2efd01', 'country', 'Royaume de Belgique', 'royaume de belgique', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2be7137f-3634-3919-c195-721bde3bb4e4', 'country', 'Rumania', 'rumania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b4d9e93a-aa36-0359-fa78-0daea8133f6e', 'country', 'Russia', 'russia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '734e2ed9-11cb-dd15-a333-1d65deddde42', 'country', 'Russian Federation', 'russian federation', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd1fad382-5ee9-fce2-ec86-19fda6e2ea63', 'country', 'Rwanda', 'rwanda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c79aa873-f30e-5a47-2124-72b06c5bf75b', 'country', 'Rzeczpospolita Polska', 'rzeczpospolita polska', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fd0acf5e-fe84-b8b2-3652-b09e34193f81', 'country', 'République Gabonaise', 'republique gabonaise', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e7a3c272-a1ca-5f72-0ddc-a3fa7163b551', 'country', 'République Togolaise', 'republique togolaise', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fd4b3161-4033-6602-3a42-c7d10b46943d', 'country', 'République centrafricaine', 'republique centrafricaine', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '33f632ce-998e-d242-bf65-168a47da0cc9', 'country', 'République d''Haïti', 'republique d''haiti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9be5a398-e36a-7450-6893-fbd7b3adcbfc', 'country', 'République de Côte d''Ivoire', 'republique de cote d''ivoire', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '83c73933-8d51-2fcd-5884-3994545eb3f4', 'country', 'République de Djibouti', 'republique de djibouti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '225ae7fa-7205-83aa-56ee-b3067f482384', 'country', 'République de Guinée', 'republique de guinee', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2e905ca7-020f-ef99-95fe-32311d118287', 'country', 'République de Guinée équatoriale', 'republique de guinee equatoriale', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '274a5baf-88dd-5a72-9e8b-292601a7f0f9', 'country', 'République de Madagascar', 'republique de madagascar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7f5cc680-f68d-9b8e-56b9-f2923712a4d2', 'country', 'République de Maurice', 'republique de maurice', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '53bf6fcb-f5fb-e257-7163-ac87d4f00cdb', 'country', 'République de Vanuatu', 'republique de vanuatu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9b80bbe0-c049-9ea5-4068-3bcafe845b5c', 'country', 'République des Seychelles', 'republique des seychelles', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '40045cb3-eb52-395a-e873-99a0b9453ad1', 'country', 'République du Burundi', 'republique du burundi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f10d69ee-f388-06e0-73c5-eda9b2bb3c77', 'country', 'République du Bénin', 'republique du benin', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e40a9f29-7650-4267-1011-c8cd69a939fc', 'country', 'République du Cameroun', 'republique du cameroun', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7f55686a-aa2a-6dcb-a1cc-53ff3eab445f', 'country', 'République du Mali', 'republique du mali', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '425273eb-61e9-e014-14e6-47c5689edfd2', 'country', 'République du Rwanda', 'republique du rwanda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f374e3a4-f5dd-8d19-1dca-3d8fed073cad', 'country', 'République du Sénégal', 'republique du senegal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '025f6811-920a-1aca-5b71-2d5c2827e625', 'country', 'République du Tchad', 'republique du tchad', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cf7e9660-49d6-b4ff-0ef1-d6578ff39922', 'country', 'République française', 'republique francaise', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'dec763f8-ef88-2957-1523-bad4a4928312', 'country', 'Saint Kitts and Nevis', 'saint kitts and nevis', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '89e3cecf-2bf5-9467-fb0b-16412f6af175', 'country', 'Saint Lucia', 'saint lucia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9f39dcb2-c824-778a-b55d-7cecedee3af5', 'country', 'Saint Vincent and the Grenadines', 'saint vincent and the grenadines', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1c2d9565-1ffa-564c-a7b0-83a92cc03430', 'country', 'Sakartvelo', 'sakartvelo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c8405b4-779c-d22c-1bf8-e83a32380636', 'country', 'Salṭanat ʻUmān', 'saltanat ʻuman', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1ea202dd-3a68-f6c2-7b1d-fc625559420c', 'country', 'Samoa', 'samoa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3e480fa5-841e-0faf-1885-417e26e64779', 'country', 'San Marino', 'san marino', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '181b1a2c-7929-2742-1ff7-65e4162b06ad', 'country', 'Sao Tome and Principe', 'sao tome and principe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c1928714-6612-28cf-48f9-1cca4ad74214', 'country', 'Sarnam', 'sarnam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1d01ce6f-c9d5-069f-c691-bf22a1e0f701', 'country', 'Sathalanalat Paxathipatai Paxaxon Lao', 'sathalanalat paxathipatai paxaxon lao', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fff02b5d-85a9-a362-3fa5-2f3deb8ec6a4', 'country', 'Saudi', 'saudi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a62448a6-f81d-0861-482f-6863db0fe772', 'country', 'Saudi Arabia', 'saudi arabia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '46c5b542-5a6b-2fe9-5732-a3e5fe72ea8e', 'country', 'Schweiz', 'schweiz', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '15bc0105-029b-6be9-7f0c-6449f7455348', 'country', 'Senegal', 'senegal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '15d0bdac-65a4-6523-860c-a2faff1cd4c6', 'country', 'Serbia', 'serbia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '197b4aaf-5d1c-0a45-5218-de4040187d18', 'country', 'Seychelles', 'seychelles', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5eaddfca-e43a-a1e7-092b-a7fb5e6cde57', 'country', 'Shqipnia', 'shqipnia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '15b775f1-39f0-1063-370f-23aeacd3b664', 'country', 'Shqipëri', 'shqiperi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd2e68113-6c2b-1a3a-f987-feb25cd46f68', 'country', 'Shqipëria', 'shqiperia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 500 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2a51ba61-24d3-35af-e2e3-e80a4b6927fd', 'country', 'Sierra Leone', 'sierra leone', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd110af32-1df0-3fe2-ef7f-a12f27634a28', 'country', 'Singapore', 'singapore', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '54a2eff9-2101-56d8-20bd-bc30ac18894b', 'country', 'Singapura', 'singapura', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9e27b2f9-2279-5d84-204d-ede46f83791e', 'country', 'Slovak Republic', 'slovak republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6c5a8f2c-6a58-0db8-60c5-d17e0f247df2', 'country', 'Slovakia', 'slovakia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ef2d7c0f-5a31-5d70-4766-a585d99150db', 'country', 'Slovenia', 'slovenia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '402fa3e5-703c-313c-504e-343e623b40c8', 'country', 'Slovenská republika', 'slovenska republika', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '736f8f6a-be9f-ec1e-e414-6cf0db9a7f6c', 'country', 'Socialist Republic of Vietnam', 'socialist republic of vietnam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '96eb93cb-cde9-4ec9-11ca-2988bf451480', 'country', 'Solomon Islands', 'solomon islands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6dbec967-b633-c23a-66ca-4ce0c3417739', 'country', 'Somalia', 'somalia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c6fed73d-1e15-1429-3492-185a10e5f6da', 'country', 'South Africa', 'south africa', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b99dd1ca-bf89-be22-6d39-096af5252f5c', 'country', 'South Korea', 'south korea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5da7189c-b25d-ec2c-4b90-9cf87c66dc83', 'country', 'South Sudan', 'south sudan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '339710f3-1aae-0873-af32-f3d2ab36b972', 'country', 'Spain', 'spain', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '703655be-5474-f8a3-053a-0788428db1d5', 'country', 'Sranangron', 'sranangron', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9f7b483a-dcfd-2bcf-2275-00d5b2ced1d1', 'country', 'Srbija', 'srbija', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'fc223a5c-07bd-1314-66bf-a91d775b1d42', 'country', 'Sri Lanka', 'sri lanka', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '55ef3b20-12f6-17ed-1006-d27a98c90925', 'country', 'State of Eritrea', 'state of eritrea', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '11f2eb19-7323-56b5-88bc-f696cc35284c', 'country', 'State of Israel', 'state of israel', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c0e13cb-b2cf-9ddf-3549-c36172baf0c5', 'country', 'State of Kuwait', 'state of kuwait', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '17e1a296-daa8-c80e-860d-536f7df67efc', 'country', 'State of Libya', 'state of libya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5635a5aa-095c-704e-992d-f90cf817ca55', 'country', 'State of Qatar', 'state of qatar', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b7996896-0d03-eb92-2c9a-e3ede223d4a3', 'country', 'Stato della Città del Vaticano', 'stato della citta del vaticano', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'cf9dc452-313d-1de5-3c59-27682b401ca8', 'country', 'Sudan', 'sudan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c82893f7-37dc-097b-876b-3c3069613045', 'country', 'Suid-Afrika', 'suid-afrika', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e0b61b31-415c-24d3-f7e2-f528be0ac1f0', 'country', 'Suisse', 'suisse', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4534b5be-0371-41c0-f595-ee834714e6c7', 'country', 'Sultanate of Oman', 'sultanate of oman', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '5e3d35d8-d957-78f6-590f-1cf1ad9b11cb', 'country', 'Suomen tasavalta', 'suomen tasavalta', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3bd2a2c9-35c9-2cc0-ec48-770b652f99f4', 'country', 'Suomi', 'suomi', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9c3053e7-a635-8116-50de-16399500ea4a', 'country', 'Suriname', 'suriname', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '96c79449-7805-d99a-0dd8-bb9905a66ffc', 'country', 'Svizra', 'svizra', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8025bde9-15ee-2c87-617d-a29bc9e241b7', 'country', 'Svizzera', 'svizzera', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'acbab940-1312-9fac-c147-b49f9b21d4b5', 'country', 'Swatini', 'swatini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3a447da7-6bbc-67c8-4179-8d0743a96ebb', 'country', 'Swaziland', 'swaziland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b4440742-af86-f325-cc83-2eddf97cde45', 'country', 'Sweden', 'sweden', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ff5a5400-f102-5d10-4cbc-47ed300f74a6', 'country', 'Swiss Confederation', 'swiss confederation', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2ec91950-82dd-c4c3-0a7f-363216d88c2c', 'country', 'Switzerland', 'switzerland', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '36ec168a-c8ec-00e4-ab9e-9767258310bd', 'country', 'Syria', 'syria', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '414a93a1-26cb-a02c-ecda-2f4b6e16da53', 'country', 'Syrian Arab Republic', 'syrian arab republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '52c3cef4-6abe-2394-0258-e85b3ec8bc98', 'country', 'São Tomé and Príncipe', 'sao tome and principe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '824b4158-82a8-d376-9580-6aee0d24802a', 'country', 'Tajikistan', 'tajikistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4e9eaf66-2b25-5e6d-9efc-9fc31c0d7c26', 'country', 'Tanzania', 'tanzania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'dfecf71c-24bf-ef9b-2908-86d40a356321', 'country', 'Tchad', 'tchad', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3b827259-4aa2-d370-a316-3a85f83fe88b', 'country', 'Tetã Paraguái', 'teta paraguai', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4d77c36f-7a37-9720-5499-92a0bf8181ae', 'country', 'Tetã Volívia', 'teta volivia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c426a244-3c8e-1808-2ecd-4cc29b382e97', 'country', 'Thai', 'thai', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '55f5a301-f639-0681-d65a-329ef357514a', 'country', 'Thailand', 'thailand', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7c95c87a-7537-8ed4-e68b-05b24f1ad39b', 'country', 'The Former Yugoslav Republic of', 'the former yugoslav republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '51850cfc-53c2-cf5c-78c5-c0c663a945ee', 'country', 'The Netherlands', 'the netherlands', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c87330d7-5dc2-074e-c5a2-ac7cc96a8b6f', 'country', 'The former Yugoslav Republic of Macedonia', 'the former yugoslav republic of macedonia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 550 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1580341a-2f33-03a3-5741-3e253f26663f', 'country', 'Timor Lorosae', 'timor lorosae', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '3095efe6-f1db-9a64-c870-251f45f3278a', 'country', 'Timor-Leste', 'timor-leste', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e987b9a1-82bb-e559-66cb-8d147c65025c', 'country', 'Timór Lorosa''e', 'timor lorosa''e', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '51300348-ef19-56a8-d5b8-2db47f1fc42d', 'country', 'Togo', 'togo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4bbd8845-5122-ff22-32ae-a27a8867d0a1', 'country', 'Togolese', 'togolese', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd28644f4-9f63-f7b7-44ee-020492bf3620', 'country', 'Togolese Republic', 'togolese republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '323228c1-f515-8751-e582-6414d02204ee', 'country', 'Tonga', 'tonga', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0725c543-71d0-845a-cf81-2be5d0d9e407', 'country', 'Toçikiston', 'tocikiston', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a9ee5a3d-3f17-25f7-f9f4-72882ff4bd67', 'country', 'Trinidad and Tobago', 'trinidad and tobago', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4ea84a05-d9f8-4e81-f510-cca5ea0abdef', 'country', 'Tunisia', 'tunisia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '42e3fab1-866d-5549-2375-5c64c6026642', 'country', 'Turkey', 'turkey', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '97dfc355-dda4-eb72-5abc-411aef4a16df', 'country', 'Turkiye', 'turkiye', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'eebc973c-09ac-30e3-2fc0-5f5ee9106ec7', 'country', 'Turkmenistan', 'turkmenistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f604650f-80e4-9869-f27a-abf0a01a9be8', 'country', 'Tuvalu', 'tuvalu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '85b9e6d8-5c88-34ed-6bd0-52d4efa995c1', 'country', 'Türkiye Cumhuriyeti', 'turkiye cumhuriyeti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'addf5950-7a01-8990-db00-e0d4537c7f68', 'country', 'Udzima wa Komori', 'udzima wa komori', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4c4b26b1-6c9e-2e86-29e3-ad3682e1aa2d', 'country', 'Uganda', 'uganda', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '20939682-869a-ebea-23e6-73e43c93b967', 'country', 'Ukraine', 'ukraine', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f98e5de5-bf70-fa79-e969-5b1a214c7dc4', 'country', 'Ukrayina', 'ukrayina', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9ffedcdf-734a-ad49-93c2-38c7b65da5df', 'country', 'Umbuso weSwatini', 'umbuso weswatini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd7e8e824-7c7c-b8d7-ca7d-67cbe8db5761', 'country', 'Union des Comores', 'union des comores', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'a9f0433d-d9a4-3826-ee59-8f81955571cf', 'country', 'Union of the Comoros', 'union of the comoros', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8f8793d2-26ea-00f2-665a-e19481997f32', 'country', 'United Arab Emirates', 'united arab emirates', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '9734a6f1-e805-45d9-1b71-700b19e6f5af', 'country', 'United Kingdom', 'united kingdom', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '03bbdfe6-c13b-6d88-c144-c51f2fd0f8bc', 'country', 'United Mexican States', 'united mexican states', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '21d05b82-ab68-fa0b-ed45-ec5c7947f3d2', 'country', 'United Republic of', 'united republic of', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8bd89ea0-108e-fd17-aafa-7895adde4f11', 'country', 'United Republic of Tanzania', 'united republic of tanzania', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '84d41b0a-37e4-719e-8974-7033e0d8cc00', 'country', 'United States', 'united states', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6ef6b83d-a3f1-a665-115f-909d0e2684fc', 'country', 'United States of America', 'united states of america', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '96765ccb-3f2b-bcc4-93a2-81a803d2b826', 'country', 'Uruguay', 'uruguay', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '75a03c77-143b-5a78-7cb1-defa9ffc105a', 'country', 'Uzbekistan', 'uzbekistan', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ff781959-ddae-abf2-cb5b-4feaa70c577d', 'country', 'Vanuatu', 'vanuatu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '044dc326-59b9-7f7d-5d2f-a399abed9bfb', 'country', 'Vatican City', 'vatican city', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'aaa7b19e-32f2-7bdb-f0f3-6c0b87422572', 'country', 'Vatican City State', 'vatican city state', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'be8816fe-cf05-0d19-e345-1019c0c9ea5b', 'country', 'Venezuela', 'venezuela', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'd53ba18e-9a7a-f161-8c06-b5114b9097a5', 'country', 'Viet Nam', 'viet nam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0aa0c429-d074-7efd-a352-24470c90cf3a', 'country', 'Vietnam', 'vietnam', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e7da4c3f-66ec-41e0-4ecc-2d89248dafa9', 'country', 'Viti', 'viti', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ce517e54-16ee-9903-5baa-7e81be0a5307', 'country', 'Wai‘tu kubuli', 'wai‘tu kubuli', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '63b176c1-3839-b78e-a803-7ffd69e3015e', 'country', 'Wuliwya', 'wuliwya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ca27ed65-b864-85bb-45be-4d29e6663a5c', 'country', 'Wuliwya Suyu', 'wuliwya suyu', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e88ce331-4f2a-f2f5-2909-40429d8a796c', 'country', 'Yemen', 'yemen', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8f288d23-028a-d25c-f4dc-6810814e15cd', 'country', 'Yemeni Republic', 'yemeni republic', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '899f9c51-3ee4-b4d1-c8c6-a4be7a06e3e0', 'country', 'Zambia', 'zambia', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c240200c-7f88-d8d8-ee0e-e696688d1df5', 'country', 'Zhongguo', 'zhongguo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8d503905-7787-020c-b6be-946ef8b3c097', 'country', 'Zhonghua', 'zhonghua', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2468e334-bf0b-6008-f0a3-d4921150da7e', 'country', 'Zhōngguó', 'zhongguo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c906d462-f85f-183d-408a-a345562543cb', 'country', 'Zhōnghuá Rénmín Gònghéguó', 'zhonghua renmin gongheguo', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1c9104f7-26c4-b545-5325-da38f26c9cfe', 'country', 'Zimbabwe', 'zimbabwe', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2cfde4a7-ab51-33ff-0033-8ebdc1901bf2', 'country', 'al-Ittiḥād al-Qumurī', 'al-ittihad al-qumuri', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- 600 entities inserted so far
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6bd4dead-0f32-2714-0a98-81695ecd0c3d', 'country', 'al-Jumhūriyyah al-Yamaniyyah', 'al-jumhuriyyah al-yamaniyyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '7500a590-a723-0459-c1af-4135127955dd', 'country', 'al-Jumhūriyyah al-ʾIslāmiyyah al-Mūrītāniyyah', 'al-jumhuriyyah al-ʾislamiyyah al-muritaniyyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '68100279-c5f7-5478-db12-40e7334c8ab1', 'country', 'al-Jumhūriyyah at-Tūnisiyyah', 'al-jumhuriyyah at-tunisiyyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e4290c4b-15b7-9685-c75a-f30be694dcc3', 'country', 'al-Mamlakah al-Urdunīyah al-Hāshimīyah', 'al-mamlakah al-urduniyah al-hashimiyah', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '03b0c8ad-74fd-76ef-0ec3-8664617995fc', 'country', 'aṣ-Ṣūmāl', 'as-sumal', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '787e2fc1-1acb-3f7b-5933-e1cddd51c310', 'country', 'ilaṅkai', 'ilankai', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c7e5b6c9-0718-2961-4360-142011746265', 'country', 'the Abode of Peace', 'the abode of peace', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8837d82c-306f-03c7-f2a8-0c01a4211432', 'country', 'the Democratic Republic of the', 'the democratic republic of the', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c69a6941-2a6c-7a5e-a119-43a8d49e2e8e', 'country', 'weSwatini', 'weswatini', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'becd0a35-df00-85f7-64be-7760db74398a', 'country', 'Çumhuriyi Toçikiston', 'cumhuriyi tocikiston', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '09b6d83b-bb23-c141-ec28-1eaf71d99bd6', 'country', 'Éire', 'eire', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '98ebb41b-6b6e-dffc-c339-28ccee03057d', 'country', 'Česko', 'cesko', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '61feb5a8-a19a-226a-52bb-cff120922f5f', 'country', 'Česká republika', 'ceska republika', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '597ba6c1-58c0-dfcf-ccfa-66f69557decc', 'country', 'ʁɛpublika de an''ɡɔla', 'ʁɛpublika de an''ɡɔla', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '290b9803-cf73-83cb-d56a-042ef340ac30', 'country', 'ʾErtrā', 'ʾertra', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0442634f-06d8-d3d3-b658-15f44c1ac2a7', 'country', 'ʾĪtyōṗṗyā', 'ʾityoppya', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6b9e4653-36d1-ce92-d8be-cbe31e7f9f7f', 'country', 'Ελληνική Δημοκρατία', 'ελληνικη δημοκρατια', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1c44189d-94b0-7a43-b34a-83f6199a8418', 'country', 'Κυπριακή Δημοκρατία', 'κυπριακη δημοκρατια', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e588b6a9-20de-8534-2e93-2c997cb298d3', 'country', 'Ўзбекистон Республикаси', 'узбекистон республикаси', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '75aeaec1-3337-255c-96a5-fcceffa814f8', 'country', 'Белоруссия', 'белоруссия', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1566e29f-be9e-4fdd-12fc-dc4ba65e6578', 'country', 'Босна и Херцеговина', 'босна и херцеговина', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '10e3e7c3-e154-344f-4526-714104ce870a', 'country', 'Казахстан', 'казахстан', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c43d25a8-772f-eafd-102c-fed0fb747cc5', 'country', 'Киргизия', 'киргизия', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'c3dbbb22-450d-4e3f-2d53-f072777e68bd', 'country', 'Кыргыз Республикасы', 'кыргыз республикасы', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '4f1efb52-254d-8539-7594-88eee448d259', 'country', 'Република България', 'република българия', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '167eeb03-085b-2818-5df2-5fe4cb8d0cfa', 'country', 'Република Косово', 'република косово', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '6285232d-9dc8-88d8-ee20-e2134403a90e', 'country', 'Република Северна Македонија', 'република северна македонија', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '1dfe3370-7cbe-51b1-6dda-364ad1d91df2', 'country', 'Република Србија', 'република србија', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f85c77eb-17d2-90c3-4086-528fbd4f1dd0', 'country', 'Республика Белоруссия', 'республика белоруссия', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '62653d7c-3087-4aea-099a-360ddcadd946', 'country', 'Республика Казахстан', 'республика казахстан', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b4d20063-be31-59eb-c2ec-3914da2a32c3', 'country', 'Российская Федерация', 'россииская федерация', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'e2b50b40-54ad-8566-fa7a-c97e43e30cbd', 'country', 'Қазақстан Республикасы', 'қазақстан республикасы', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '43ae12c9-33a4-7765-eaff-b83f1ab1b6f2', 'country', 'Ҷумҳурии Тоҷикистон', 'ҷумҳурии тоҷикистон', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '8aafe799-1b45-de68-1e9c-37443c79cd0d', 'country', 'Հայաստանի Հանրապետություն', 'հայաստանի հանրապետություն', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'ebaa59df-5dd1-7791-7fa2-acbfed9e6568', 'country', 'இந்தியா', 'இநதியா', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'b73b4684-d617-f4dc-8533-2dfa796bd02c', 'country', 'ราชอาณาจักรไทย', 'ราชอาณาจักรไทย', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '2524e49b-07e2-b30b-4b2f-d04bf312a331', 'country', 'ሃገረ ኤርትራ', 'ሃገረ ኤርትራ', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f6759eab-8c71-03e6-3ce4-a2bfc4e99549', 'country', 'የኢትዮጵያ ፌዴራላዊ ዲሞክራሲያዊ ሪፐብሊክ', 'የኢትዮጵያ ፌዴራላዊ ዲሞክራሲያዊ ሪፐብሊክ', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '13fa9cb9-aff3-5f51-576b-4de612adceba', 'country', '中华人民共和国', '中华人民共和国', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  'f7f40d44-93d6-b405-47ee-1cfb39d0c105', 'country', '新加坡共和国', '新加坡共和国', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
INSERT INTO entities (id, entity_type, display_name, normalized_name, created_at, updated_at) VALUES (
  '0199df58-0b55-21cf-5478-159208cf0f20', 'country', '조선민주주의인민공화국', '조선민주주의인민공화국', NOW(), NOW()
) ON CONFLICT (entity_type, normalized_name) DO NOTHING;
-- Total entities: 641

-- Question: Name a country — its population in millions is your score
-- Region filter: none, 160 countries
-- Valid answers: 160, Score pool: 3838, Viable: True
-- Zones: high=9, mid=49, checkout=102
UPDATE questions SET
  high_value_count = 9,
  mid_range_count = 49,
  checkout_count = 102,
  total_valid_count = 160,
  total_score_pool = 3838,
  single_question_viable = true,
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = '4bde928a-c9ed-f5f3-e8a6-8447eb858d85';

INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5e3265f4-4d96-ca9d-dbcc-322c34ffef3a', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mauritius', 'Mauritius', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4db82522-0b88-2e25-0147-128760c10d33', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'gambia', 'Gambia', 2, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c09cbc21-d06a-9506-a5f6-c36a05165756', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'malawi', 'Malawi', 21, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3742157b-49e3-02c0-4c38-3d997ccc60b1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'cameroon', 'Cameroon', 29, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '33bfc06b-e94c-315a-142f-284098459e23', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ivory coast', 'Ivory Coast', 32, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ci.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '53423924-a234-5074-0312-96187c5fac67', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'togo', 'Togo', 8, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/tg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4fc10637-6749-7167-ce02-9f079f0b2b2c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'libya', 'Libya', 7, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/ly.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5c9d204b-1537-c6fa-8464-8dbca867c568', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'south africa', 'South Africa', 63, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/za.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '59621031-b5d7-c8bd-35ff-8860a738eaed', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'morocco', 'Morocco', 37, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/ma.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '59ee9000-cd66-f210-5172-3907393fd615', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'djibouti', 'Djibouti', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/dj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9713e7bc-6751-c913-3cd3-2caf0a85f857', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'burundi', 'Burundi', 12, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/bi.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '02f22e07-3247-f66a-5b71-b31a453895e9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'tanzania', 'Tanzania', 68, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/tz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c605be23-d7d2-f5a2-1ffd-97d2abaf4204', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'rwanda', 'Rwanda', 14, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/rw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '947086cb-8397-8fbd-241e-2149a78859d9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mali', 'Mali', 22, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ml.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '93240b5d-4101-ab69-5394-a8c356fc0e08', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ghana', 'Ghana', 34, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '35893db2-b558-b114-2e6e-0a96129792cf', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'kenya', 'Kenya', 53, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/ke.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8fa69f4e-7588-218f-f5be-52b72aeb43ac', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'madagascar', 'Madagascar', 32, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '180cad3a-70ac-bf4e-d1d0-15979476ce27', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'eritrea', 'Eritrea', 4, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/er.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c58b1931-082d-0401-c953-4f5ccb08f1b1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'namibia', 'Namibia', 3, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/na.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0b1f4d3f-aabc-2bf7-de43-2fa9661eef15', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'dr congo', 'DR Congo', 113, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '44e43d10-06c3-8286-066d-85d87195581a', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'zimbabwe', 'Zimbabwe', 17, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/zw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cf71585d-025d-c5db-1171-23d926d81e37', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'equatorial guinea', 'Equatorial Guinea', 2, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/gq.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7834b944-74d6-615b-e9e3-c7c43f44579c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mauritania', 'Mauritania', 5, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/mr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '14ad160b-705d-5af5-410c-ed46dc41dd43', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ethiopia', 'Ethiopia', 112, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/et.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '22817f31-e374-0691-5850-34f405001a0e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'zambia', 'Zambia', 20, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/zm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '53c20f92-0252-1100-9f74-38ac909755e2', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'somalia', 'Somalia', 20, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/so.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '02aaf9f5-737b-59de-1a8c-de9e818e4abc', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'guinea', 'Guinea', 14, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '48e440f7-813c-9fd1-3326-627ebf74cf28', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'sierra leone', 'Sierra Leone', 9, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/sl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '45fbffe8-14a5-d36c-4cb5-39693461607c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'benin', 'Benin', 13, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/bj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4ca7f6cb-6d9e-ae9d-27ca-2fd40318d700', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'liberia', 'Liberia', 5, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/lr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e7667395-aca1-6418-87a2-a0ee1c04d153', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'south sudan', 'South Sudan', 16, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ss.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7a0f617d-f9c8-e9f8-d245-f3ca4cfcad5d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'republic of the congo', 'Republic of the Congo', 6, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '069ef009-f627-e454-859a-b5e77f536a10', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mozambique', 'Mozambique', 34, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5e96ea7a-9c14-305f-e738-1077fe6b6ceb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'eswatini', 'Eswatini', 1, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/sz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '74259239-a41b-f53d-2bfd-9a802a722d1c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'niger', 'Niger', 26, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ne.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '24f52bb2-2d25-adba-5632-35b8102e3d3d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'botswana', 'Botswana', 2, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/bw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '219b816b-9c83-6911-0fd1-ef61a87d5abd', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'egypt', 'Egypt', 107, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/eg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ac10a8ee-7ffd-e7f5-ee22-f1f106699f26', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'gabon', 'Gabon', 2, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ga.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2d84c206-6b4d-b466-a16d-969fdfe6b08e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'angola', 'Angola', 36, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ao.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'fa81700d-33e7-5cc7-ac6d-757774dcd113', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'uganda', 'Uganda', 46, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/ug.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0954b2a4-fde4-670d-5c66-c8b98912b116', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'chad', 'Chad', 19, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/td.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '93a6cf98-6ae5-0857-79db-73b9f7b1f128', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'algeria', 'Algeria', 47, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/dz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3e5df763-c5ab-a40a-132e-bdb4e5d250a9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'lesotho', 'Lesotho', 2, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/ls.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1c1b4e41-c012-a4e1-0b08-22ab6280f8d3', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'burkina faso', 'Burkina Faso', 24, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/bf.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cd1ffd44-fc01-be26-0ce6-a99846643d33', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'central african republic', 'Central African Republic', 6, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cf.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '688b0ab0-e7fb-fbbb-9e84-d760a70a1ccb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'senegal', 'Senegal', 19, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/sn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4ccad740-faa5-9c82-78cd-f1ee3c5ccc19', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'sudan', 'Sudan', 52, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/sd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3e7b83c5-fbfd-25a2-7cf3-b79f862c0412', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'guinea-bissau', 'Guinea-Bissau', 2, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '6ca110ba-5b22-d477-8408-0e04e65c2df1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'tunisia', 'Tunisia', 12, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/tn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '80054b97-3414-bae1-7be1-76d21ba49cba', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'comoros', 'Comoros', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/km.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1c0f9501-d75f-4efd-9bbc-bd2cd95b5fb1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'slovenia', 'Slovenia', 2, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/si.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '96e7333f-14a5-9ed0-bb21-975b7f00ad85', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'sweden', 'Sweden', 11, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/se.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '89b907a4-ef92-9099-7138-2ae0bd1bdffc', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'switzerland', 'Switzerland', 9, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/ch.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'eb1e1d8f-11cf-0a9e-d33f-a493368da5c8', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'poland', 'Poland', 37, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/pl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f8bf5bd1-9e7b-f336-a085-420bff7e1108', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'france', 'France', 66, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/fr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3c2c44d2-7c68-b9e4-37fa-f6c76bc7900e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'netherlands', 'Netherlands', 18, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/nl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '6d54966f-2504-dbd6-33b6-126d3b976495', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'montenegro', 'Montenegro', 1, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/me.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f7654b10-b841-4dce-983a-4052bef5fdee', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'kosovo', 'Kosovo', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/xk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '10a2c0e2-8399-c6e2-57aa-e92cc47e6bec', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bulgaria', 'Bulgaria', 6, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/bg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2a741a1f-fcfd-0ee6-1f38-521be5cddf24', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'united kingdom', 'United Kingdom', 69, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/gb.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '560303d0-b1df-fc52-918f-1c2330c2675c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'malta', 'Malta', 1, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/mt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '45b79c7e-a7c2-31bd-3722-5170211c7b9b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'estonia', 'Estonia', 1, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/ee.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b1ab1739-087e-7cbe-ed61-062a5500a38b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'denmark', 'Denmark', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/dk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0f0be99b-6963-a310-776a-e6602276f064', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'greece', 'Greece', 10, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/gr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '49d7b768-9a96-e4f5-d9c0-a9488e38f6e8', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'czechia', 'Czechia', 11, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/cz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'afad39f6-0cff-07de-20ce-5d63921f4806', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'cyprus', 'Cyprus', 1, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/cy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2e421f47-0da5-eefb-6ada-5f3b32ada5a0', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'latvia', 'Latvia', 2, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/lv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '29e70298-5c8c-6c7b-712e-73eae3a5f446', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'germany', 'Germany', 83, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/de.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cec3e61d-0882-122b-a114-6dcab34ccd5f', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bosnia and herzegovina', 'Bosnia and Herzegovina', 3, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/ba.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ae084b5b-3f25-aee4-778a-7bd9982e54a1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'italy', 'Italy', 59, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/it.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e4eb8e47-2c43-21bb-5b1e-c33cdfb784c4', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'romania', 'Romania', 19, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/ro.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a745526a-fb2e-9c41-ad2d-c8c03833a80e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ireland', 'Ireland', 5, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/ie.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0fc4deab-06f2-b188-dc62-b25b993fd82e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'luxembourg', 'Luxembourg', 1, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/lu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f805a06c-17e1-65f4-eb4c-fc3e22337fbd', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'austria', 'Austria', 9, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/at.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7e020035-10e5-3a90-eb1f-2751b2064825', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'portugal', 'Portugal', 11, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/pt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5b564512-30cf-236c-cc16-9abdabdcb5fe', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'north macedonia', 'North Macedonia', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/mk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '376ca308-ed67-1118-f6c8-f0bd29636f76', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'serbia', 'Serbia', 7, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/rs.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c2090cfa-e86c-a0db-8c13-a5a8765fd5a9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'moldova', 'Moldova', 3, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/md.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c010dc2a-4990-8ff5-0918-44e5806b0652', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'hungary', 'Hungary', 10, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/hu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b0cfdc4b-c3f1-8e29-07b6-c24dc6d42897', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'lithuania', 'Lithuania', 3, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/lt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b46e425a-87ee-8de2-c65d-a4df5a7aa523', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'norway', 'Norway', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/no.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bfe1c20c-ff75-b7be-2ec1-5668c3c18d7b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'albania', 'Albania', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/al.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8cfd54c2-6a60-d902-5b33-e5cd89ec43c5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'slovakia', 'Slovakia', 5, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/sk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2b4ac27c-4517-47c1-c1dd-c6c904b32d4c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'belarus', 'Belarus', 9, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/by.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ae4fb6a3-53ad-6afb-193a-1be5db3cdb53', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'belgium', 'Belgium', 12, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/be.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '947b5f94-187e-78aa-e543-3056bc60e837', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'croatia', 'Croatia', 4, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/hr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '60544c9f-ece8-5710-3d3b-d5acd894784d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'finland', 'Finland', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/fi.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9a4eb655-f42c-baa4-8f7f-79bec44124ab', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ukraine', 'Ukraine', 33, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/ua.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a33b0817-725b-507d-46a8-2e34bbe06441', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'russia', 'Russia', 146, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/ru.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '69db9641-1936-1ce7-4165-7635014dea96', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'spain', 'Spain', 49, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/es.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '6aadaba7-923e-375c-18de-edbba695cdeb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'qatar', 'Qatar', 3, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/qa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0d9512b5-ada7-a706-0951-b749f6ca2fa5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'myanmar', 'Myanmar', 51, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/mm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '53831eee-f3eb-311e-9fa2-f68207c36ca9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'jordan', 'Jordan', 12, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/jo.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3ef7f55c-b367-a4c3-4b07-2e28c6de7bc0', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'laos', 'Laos', 8, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/la.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '430da673-40e3-2b6e-6ea7-9d3e258cf29c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bhutan', 'Bhutan', 1, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/bt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0d4a1d66-007f-2be7-3146-d99cf7f2a086', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'uzbekistan', 'Uzbekistan', 38, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/uz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '28862055-dcd6-3c3b-62e6-802d12cc8cb8', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'timor-leste', 'Timor-Leste', 1, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/tl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0783c0c0-b82c-8bfd-8d7a-ceb24ced4890', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bangladesh', 'Bangladesh', 170, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/bd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5a05084f-d733-61ee-3309-0e706f5b78ac', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'kyrgyzstan', 'Kyrgyzstan', 7, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/kg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9cee4d43-43e6-4e59-01fb-80782ee3f2d5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'iran', 'Iran', 86, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/ir.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7a62bf9b-9ad2-87ba-ef80-e2ac68688e39', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bahrain', 'Bahrain', 2, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/bh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '98a3aacf-c1a0-25ad-9716-26971e2a5745', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'turkey', 'Turkey', 86, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/tr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0f6c795f-c570-dc3d-6357-cee64018cf0c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'south korea', 'South Korea', 51, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/kr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cd364252-6b1c-b31f-39ab-857a0489effd', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'lebanon', 'Lebanon', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/lb.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '58f9c81e-618c-4c2c-6d7e-e78542282c0d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'georgia', 'Georgia', 4, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ge.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a2328150-fe2a-daf4-2afc-c2068213202a', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'tajikistan', 'Tajikistan', 10, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/tj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '63e80f10-1be4-aa18-0665-8bd8656ebc33', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'thailand', 'Thailand', 66, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/th.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '89d8cafe-b58f-9726-d491-f023caefb882', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'malaysia', 'Malaysia', 34, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/my.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '83860588-2d26-cdca-4a6a-47827b118c6a', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'north korea', 'North Korea', 26, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/kp.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3f822c45-82e5-4ac7-5b22-8861e0922d72', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'saudi arabia', 'Saudi Arabia', 35, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/sa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a05aee67-b66d-1efd-ca89-e5acf74beec5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mongolia', 'Mongolia', 4, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/mn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e07daf70-d6d0-92b2-5318-5f1b5099f2b5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'armenia', 'Armenia', 3, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/am.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5031bed0-ad07-57a0-07e0-a2567d84188b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'iraq', 'Iraq', 46, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/iq.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '09bab6f8-4862-1c95-70a0-185c219e2f47', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'yemen', 'Yemen', 33, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ye.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ed87e2fa-e159-1460-39b1-eca280cf9de6', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'japan', 'Japan', 123, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/jp.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'd096c94f-1cd2-8040-138e-5ecf2d9be442', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'israel', 'Israel', 10, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/il.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3f88b0a9-7174-fa4a-0dc4-572698e9034f', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'afghanistan', 'Afghanistan', 44, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Flag_of_the_Taliban.svg/320px-Flag_of_the_Taliban.svg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '89b8ee16-fa91-fda1-6602-8ba1825897ec', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'cambodia', 'Cambodia', 18, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/kh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5d2147d5-f0e5-878a-ebbd-12ce39f2d7ae', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'sri lanka', 'Sri Lanka', 22, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/lk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a0d592b8-6fca-d3a3-a7f7-7ba1d009abee', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'turkmenistan', 'Turkmenistan', 7, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/tm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '749af81e-0c48-5afa-334d-4d0440343838', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'oman', 'Oman', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/om.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '14b5f056-f222-20d3-0088-0de044bd8cf1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'azerbaijan', 'Azerbaijan', 10, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/az.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2719ab40-cf5a-a28f-307c-6361dca60c4a', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'maldives', 'Maldives', 1, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/mv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cae4b544-dd27-c659-1d50-b0fa925b7b9b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'vietnam', 'Vietnam', 101, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/vn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '38e3a6cf-cbfd-2f70-13ca-5c27681211dd', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'singapore', 'Singapore', 6, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/sg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '02351c49-eef5-55a4-dacd-fa3667fad13b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'kazakhstan', 'Kazakhstan', 20, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/kz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '205ee95f-d74d-35b2-66b4-a01c7def3271', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'philippines', 'Philippines', 114, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/ph.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '50dd9f6d-91f8-3497-2ea6-3681b15f38b5', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'syria', 'Syria', 26, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/sy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '35b2a791-c499-8d9b-5a8a-beb485b51afd', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'nepal', 'Nepal', 30, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/np.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9874f31a-f642-4c04-510e-8015a75110fa', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'kuwait', 'Kuwait', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/kw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f998d346-71b9-159e-ca6e-395f66b4ed7f', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'united arab emirates', 'United Arab Emirates', 11, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ae.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8f421db3-6f9c-506e-ee7e-8c3479a6b93c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'guatemala', 'Guatemala', 18, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/gt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '16e330e4-1f84-4156-1ecf-4a35c9af390f', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'mexico', 'Mexico', 131, true, false, '{"region": "Americas", "subregion": "North America", "flag": "https://flagcdn.com/w320/mx.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '806d4a73-edf5-7e8c-d0c8-c7e027435eb1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'bolivia', 'Bolivia', 11, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/bo.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7246a361-bc30-5d89-b217-832006bd7803', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'chile', 'Chile', 20, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/cl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7c21d7b3-8159-8126-0013-bce7b030ab7e', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'ecuador', 'Ecuador', 18, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ec.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9cfdc0ac-1e6f-d952-3a39-7d57df778c64', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'paraguay', 'Paraguay', 6, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/py.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'da685b61-7e08-3e61-ab31-60068736c9cb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'peru', 'Peru', 34, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/pe.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ada5a80e-8b07-506d-714d-cb902a1657c1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'argentina', 'Argentina', 47, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ar.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1a8fdf98-fdd3-3237-7720-dc0eb7aaf528', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'canada', 'Canada', 42, true, false, '{"region": "Americas", "subregion": "North America", "flag": "https://flagcdn.com/w320/ca.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'aacc836d-9df3-3c93-ed3d-ddc4328538fb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'panama', 'Panama', 4, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/pa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '935decd3-fd77-0f48-e4c0-ec6930c9ec4c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'nicaragua', 'Nicaragua', 7, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/ni.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '0edd4675-49fe-7171-104b-bb1cee7e458b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'dominican republic', 'Dominican Republic', 11, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/do.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '45c0e3d6-4bac-84eb-e2c9-e7f37f4c7c3d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'suriname', 'Suriname', 1, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/sr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b20b9129-b030-ba6e-c77b-f473424476ad', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'trinidad and tobago', 'Trinidad and Tobago', 1, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/tt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3a7aeab8-b014-a8e3-7813-7d69114231a1', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'colombia', 'Colombia', 53, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/co.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e4dcf387-00bc-fc5f-c349-4681675c425d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'costa rica', 'Costa Rica', 5, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/cr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cbf5a108-f0c2-8fa8-fdb2-21711604f39c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'haiti', 'Haiti', 12, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/ht.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '17f960e6-c1a4-f9bb-57b9-fd73ef120378', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'guyana', 'Guyana', 1, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/gy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4213448a-8ea5-b7a0-35eb-032067685970', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'el salvador', 'El Salvador', 6, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/sv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '36ce6833-2999-615f-e5f8-826a3e6f69ef', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'jamaica', 'Jamaica', 3, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/jm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '972d0aca-6663-b734-224d-800df1aaba6b', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'uruguay', 'Uruguay', 3, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/uy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f41c37e9-eac5-3bc1-227e-b9fd71976fab', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'venezuela', 'Venezuela', 29, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ve.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2e9a4caf-883d-1c84-ef73-e787b82a6aeb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'cuba', 'Cuba', 10, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/cu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4891f7ad-47dd-4c01-f7b3-1f43efe529c8', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'honduras', 'Honduras', 10, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/hn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e53f5516-f715-f9f3-5f39-20833fdf0c6c', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'australia', 'Australia', 28, true, false, '{"region": "Oceania", "subregion": "Australia and New Zealand", "flag": "https://flagcdn.com/w320/au.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bb39bca3-14a6-ec80-eb2b-f3613c88311d', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'solomon islands', 'Solomon Islands', 1, true, false, '{"region": "Oceania", "subregion": "Melanesia", "flag": "https://flagcdn.com/w320/sb.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bcf60721-3fd7-9d1e-0236-500cdeaedd20', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'new zealand', 'New Zealand', 5, true, false, '{"region": "Oceania", "subregion": "Australia and New Zealand", "flag": "https://flagcdn.com/w320/nz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c12bbe41-7b19-0960-a04a-74517b6222a9', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'fiji', 'Fiji', 1, true, false, '{"region": "Oceania", "subregion": "Melanesia", "flag": "https://flagcdn.com/w320/fj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ebeec09f-afc1-0928-4c1f-2fe8750991fb', '4bde928a-c9ed-f5f3-e8a6-8447eb858d85', 'papua new guinea', 'Papua New Guinea', 12, true, false, '{"region": "Oceania", "subregion": "Melanesia", "flag": "https://flagcdn.com/w320/pg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
-- 160 answers inserted for question 4bde928a-c9ed-f5f3-e8a6-8447eb858d85

-- Question: Name a country in Africa — its population in millions is your score
-- Region filter: Africa, 50 countries
-- Valid answers: 50, Score pool: 1233, Viable: True
-- Zones: high=3, mid=19, checkout=28
UPDATE questions SET
  high_value_count = 3,
  mid_range_count = 19,
  checkout_count = 28,
  total_valid_count = 50,
  total_score_pool = 1233,
  single_question_viable = true,
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = 'a10296e6-6526-47a6-db85-a6fc81a0d51b';

INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7882d3e9-cda9-73e4-cb11-751e890347ad', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'mauritius', 'Mauritius', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '063f5211-5866-3fa0-5fd1-5b2be4a3a063', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'gambia', 'Gambia', 2, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '71947b83-d432-f3eb-e0c8-ef5aa4a07e28', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'malawi', 'Malawi', 21, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'fbd679b3-f5ef-988b-6038-b96265ace03f', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'cameroon', 'Cameroon', 29, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '62e2f115-94d3-db69-7340-4937db713238', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'ivory coast', 'Ivory Coast', 32, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ci.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9010ed94-46fb-74a5-c286-22f62b8470f8', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'togo', 'Togo', 8, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/tg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f5b3367f-2ca3-3632-a16c-b4c925a06862', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'libya', 'Libya', 7, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/ly.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8880f6a7-7c85-8a2e-2c94-162c80406f15', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'south africa', 'South Africa', 63, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/za.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '034d1406-f93d-436e-1f61-755a5b3c6d9b', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'morocco', 'Morocco', 37, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/ma.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4b20b14c-0ced-07a3-6d79-a5766b603e08', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'djibouti', 'Djibouti', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/dj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9e22ae14-4861-19b3-d9d7-fda5d735f7f3', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'burundi', 'Burundi', 12, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/bi.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2c92d97d-f4e3-afb3-676a-c36a615f517b', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'tanzania', 'Tanzania', 68, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/tz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ef47f330-f2f3-6fc8-975d-cbb408439169', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'rwanda', 'Rwanda', 14, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/rw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '64d8bd52-c6ee-84d8-ca9d-b3985e336682', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'mali', 'Mali', 22, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ml.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e8448404-0e3c-d840-658b-6d5f3139b870', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'ghana', 'Ghana', 34, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cb8fe5aa-5419-0aaa-abd0-4122cf0789a1', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'kenya', 'Kenya', 53, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/ke.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '163d3345-263b-c959-50f8-d6b18f6b9db5', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'madagascar', 'Madagascar', 32, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8611b9d8-c777-9cda-bb97-7cd2a7540107', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'eritrea', 'Eritrea', 4, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/er.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b7f284f3-9234-88ed-eadb-71170871d0e2', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'namibia', 'Namibia', 3, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/na.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '959f912c-c877-113d-7f97-2c6dbc35d62e', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'dr congo', 'DR Congo', 113, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ac0f1178-cef8-adb3-8b31-523dd94788e4', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'zimbabwe', 'Zimbabwe', 17, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/zw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1e7bd14c-ded0-1d6c-8a96-1133fb2820ce', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'equatorial guinea', 'Equatorial Guinea', 2, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/gq.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c4f579e2-f045-4222-b0dc-2ff9dc4b93c4', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'mauritania', 'Mauritania', 5, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/mr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bba8503e-198a-b788-5f6b-723d59e3bd12', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'ethiopia', 'Ethiopia', 112, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/et.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8a188bdf-40e3-7ba4-fe79-b11e9ef89af2', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'zambia', 'Zambia', 20, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/zm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '62095f53-43ce-2397-a4f4-c45e0c55fe35', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'somalia', 'Somalia', 20, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/so.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '55059fcc-4f9e-8662-95f5-00e946d67f81', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'guinea', 'Guinea', 14, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '66d5a111-c652-7f80-f14b-24f18dda4da8', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'sierra leone', 'Sierra Leone', 9, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/sl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'adfd4a1d-21cf-57d8-73e6-3186acd34399', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'benin', 'Benin', 13, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/bj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3086a267-8873-d846-7af1-13f7af3902f7', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'liberia', 'Liberia', 5, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/lr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b8e02855-45d8-b71f-fd0c-1174d1e41576', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'south sudan', 'South Sudan', 16, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ss.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8116ec8f-0a55-cec4-3911-8e39640ca50a', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'republic of the congo', 'Republic of the Congo', 6, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '264414df-3756-4266-8378-bf9dd9c5e993', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'mozambique', 'Mozambique', 34, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/mz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'd2065aab-5e2c-b753-a719-e650ecf33801', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'eswatini', 'Eswatini', 1, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/sz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e32d2fbe-1276-e5d9-1143-f32a29bea507', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'niger', 'Niger', 26, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/ne.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'df042570-de1f-393a-5e4b-68d7c45af44f', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'botswana', 'Botswana', 2, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/bw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1ba5905e-bd6a-6d7b-1c3b-fe051407d6d2', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'egypt', 'Egypt', 107, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/eg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '64acf9f7-4214-a72b-d949-28d86efa5b4b', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'gabon', 'Gabon', 2, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ga.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'fa2569e7-12d2-723f-b955-d20eae2b62d4', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'angola', 'Angola', 36, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/ao.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4e500dc4-7f8c-1c15-4e38-2c65bd202b83', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'uganda', 'Uganda', 46, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/ug.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e71bf8ea-8490-7c38-eb2f-b2df1769129b', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'chad', 'Chad', 19, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/td.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c787ea90-a073-7780-42e2-bcc49753b6f1', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'algeria', 'Algeria', 47, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/dz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c88b707c-cab0-bf49-a121-97dcb8f61a8d', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'lesotho', 'Lesotho', 2, true, false, '{"region": "Africa", "subregion": "Southern Africa", "flag": "https://flagcdn.com/w320/ls.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b80791c5-6d8e-8bf6-2f3f-9ab457e3c8cd', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'burkina faso', 'Burkina Faso', 24, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/bf.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '518d3305-b9b9-c669-e194-a7ad6f4d27f0', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'central african republic', 'Central African Republic', 6, true, false, '{"region": "Africa", "subregion": "Middle Africa", "flag": "https://flagcdn.com/w320/cf.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c719abdb-1854-230e-25c8-a0a7e6a52949', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'senegal', 'Senegal', 19, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/sn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '81cb75d2-62bd-04f9-11bf-815777a732ca', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'sudan', 'Sudan', 52, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/sd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '00f776c1-531f-1d9d-d2ae-5c9fba7523c7', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'guinea-bissau', 'Guinea-Bissau', 2, true, false, '{"region": "Africa", "subregion": "Western Africa", "flag": "https://flagcdn.com/w320/gw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '65b1bb1c-790f-8a0e-887d-450c039e3024', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'tunisia', 'Tunisia', 12, true, false, '{"region": "Africa", "subregion": "Northern Africa", "flag": "https://flagcdn.com/w320/tn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a1fb8ef3-d3ae-7c77-6427-b55ca0ddb387', 'a10296e6-6526-47a6-db85-a6fc81a0d51b', 'comoros', 'Comoros', 1, true, false, '{"region": "Africa", "subregion": "Eastern Africa", "flag": "https://flagcdn.com/w320/km.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
-- 50 answers inserted for question a10296e6-6526-47a6-db85-a6fc81a0d51b

-- Question: Name a country in Americas — its population in millions is your score
-- Region filter: Americas, 24 countries
-- Valid answers: 24, Score pool: 483, Viable: False
-- Zones: high=1, mid=6, checkout=17
UPDATE questions SET
  high_value_count = 1,
  mid_range_count = 6,
  checkout_count = 17,
  total_valid_count = 24,
  total_score_pool = 483,
  single_question_viable = false,
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = 'a64937b9-1e46-4d43-c599-00e623fd5e24';

INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '53ee561e-ffaa-6c24-4be0-d3519b1e1c8b', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'guatemala', 'Guatemala', 18, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/gt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8cdeed17-c1c9-a625-82fa-cf02f6503f37', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'mexico', 'Mexico', 131, true, false, '{"region": "Americas", "subregion": "North America", "flag": "https://flagcdn.com/w320/mx.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '58246b70-7454-92fe-ec2c-1f08de5f03ed', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'bolivia', 'Bolivia', 11, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/bo.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '33d95923-0691-216b-f3dd-320c3b366e46', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'chile', 'Chile', 20, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/cl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b5c75f90-1c4d-491b-f1e0-5dd9f5e3088e', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'ecuador', 'Ecuador', 18, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ec.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '78ffbf2f-d1f7-9675-f9cb-5ee26249a555', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'paraguay', 'Paraguay', 6, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/py.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8b508c0f-d83d-968d-4b8b-ec1ecf781491', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'peru', 'Peru', 34, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/pe.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f1c66f0d-963f-6e6f-0a79-4ce7d8e7bf05', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'argentina', 'Argentina', 47, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ar.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8d6382d2-3b7c-75b8-863e-670175ec485a', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'canada', 'Canada', 42, true, false, '{"region": "Americas", "subregion": "North America", "flag": "https://flagcdn.com/w320/ca.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c36045a1-7514-e968-d071-615e867ad6f5', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'panama', 'Panama', 4, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/pa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'd9a43f78-4678-5c20-3d8c-9e4ef7a97408', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'nicaragua', 'Nicaragua', 7, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/ni.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '485f6bea-3b46-98bd-842f-e0e70fe81c2d', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'dominican republic', 'Dominican Republic', 11, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/do.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '6bc48a38-bf8a-3578-7e28-ce4178014c30', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'suriname', 'Suriname', 1, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/sr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a385d1a6-e2dd-3366-2f01-629a88dafc72', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'trinidad and tobago', 'Trinidad and Tobago', 1, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/tt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '611426fe-a7e8-7be0-b477-5c6af42cc363', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'colombia', 'Colombia', 53, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/co.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '103d5273-d845-5ae9-0ae5-8c3278b01ecc', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'costa rica', 'Costa Rica', 5, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/cr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '283f5998-af5b-c7ca-7ef3-92ffef653a57', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'haiti', 'Haiti', 12, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/ht.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8dfc09b9-d503-b804-50c2-e1560a3d0333', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'guyana', 'Guyana', 1, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/gy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '531c8f96-0b8d-29a3-a499-66135917c594', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'el salvador', 'El Salvador', 6, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/sv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '600e2171-7c83-1526-d5f4-2a1aa8bebd82', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'jamaica', 'Jamaica', 3, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/jm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '2a1fc657-259a-9a29-d4d0-83994d5525f7', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'uruguay', 'Uruguay', 3, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/uy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3174fb14-00ee-ef8f-37f7-02aad4e253ca', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'venezuela', 'Venezuela', 29, true, false, '{"region": "Americas", "subregion": "South America", "flag": "https://flagcdn.com/w320/ve.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9e566e7a-a20f-7d89-3ffa-424187772bdd', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'cuba', 'Cuba', 10, true, false, '{"region": "Americas", "subregion": "Caribbean", "flag": "https://flagcdn.com/w320/cu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '08880c61-b241-ec47-0988-80bcfe8a8b2f', 'a64937b9-1e46-4d43-c599-00e623fd5e24', 'honduras', 'Honduras', 10, true, false, '{"region": "Americas", "subregion": "Central America", "flag": "https://flagcdn.com/w320/hn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
-- 24 answers inserted for question a64937b9-1e46-4d43-c599-00e623fd5e24

-- Question: Name a country in Asia — its population in millions is your score
-- Region filter: Asia, 41 countries
-- Valid answers: 41, Score pool: 1335, Viable: True
-- Zones: high=4, mid=16, checkout=21
UPDATE questions SET
  high_value_count = 4,
  mid_range_count = 16,
  checkout_count = 21,
  total_valid_count = 41,
  total_score_pool = 1335,
  single_question_viable = true,
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = '8a2b9539-5c83-ed99-181d-41fdb49365a7';

INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8201d5a1-2249-6de5-5e3f-d105256a1676', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'qatar', 'Qatar', 3, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/qa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cfb29947-c2d1-9f88-8d19-1c74e61a9cdd', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'myanmar', 'Myanmar', 51, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/mm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7ac1fe94-a332-e9a0-c47a-b3dbb25b2e22', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'jordan', 'Jordan', 12, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/jo.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '698ac6c4-871a-9a35-563c-c90589cafd9f', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'laos', 'Laos', 8, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/la.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1f4d4448-e59c-50b0-8948-73a82a6d9780', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'bhutan', 'Bhutan', 1, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/bt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bc784152-781b-604a-b23f-c2e8bdadcb38', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'uzbekistan', 'Uzbekistan', 38, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/uz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '733884d4-05e3-d282-c578-3241164db517', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'timor-leste', 'Timor-Leste', 1, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/tl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c97fdc08-b33a-9389-d309-02f3bc2fb506', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'bangladesh', 'Bangladesh', 170, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/bd.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'dd0172ab-e573-b47f-57dd-4f6cf35e4c20', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'kyrgyzstan', 'Kyrgyzstan', 7, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/kg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '83a985e2-41de-9095-1f6e-d33fe1c3b20a', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'iran', 'Iran', 86, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/ir.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cd1a22e8-5e68-82af-7aa3-063ec3a0fbe7', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'bahrain', 'Bahrain', 2, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/bh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'f7945907-85a6-6787-e3c6-0a2f6fc97b26', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'turkey', 'Turkey', 86, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/tr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '76303032-4fd1-3b6d-5a7d-abacc99754e3', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'south korea', 'South Korea', 51, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/kr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3201017b-ea0f-a2d8-84a2-350893cb7770', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'lebanon', 'Lebanon', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/lb.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '01a6d489-6fd2-2022-82fd-227dc75cb542', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'georgia', 'Georgia', 4, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ge.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a13ae3a2-77ea-03c2-febc-fcb7c165efa5', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'tajikistan', 'Tajikistan', 10, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/tj.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5454e157-115d-88a3-2b6b-608b0cb3dffd', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'thailand', 'Thailand', 66, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/th.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '59896309-63e9-c36d-00ae-bd531a8ea03a', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'malaysia', 'Malaysia', 34, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/my.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'bc19c53f-3db6-6585-c283-3738383534aa', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'north korea', 'North Korea', 26, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/kp.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'babea80b-bf85-78df-545a-29326da00b32', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'saudi arabia', 'Saudi Arabia', 35, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/sa.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ce76c6e1-d67e-9bea-69ba-66ace04125f1', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'mongolia', 'Mongolia', 4, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/mn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '62523129-8159-43f8-9712-73e2523a2f6a', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'armenia', 'Armenia', 3, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/am.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5b861dae-5242-4a30-3d49-6249d5abe2e1', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'iraq', 'Iraq', 46, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/iq.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a6ea0559-0782-3596-7ab9-8164141df8ac', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'yemen', 'Yemen', 33, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ye.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '8ddcb117-914f-e196-5fee-4aeefbbe0ed1', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'japan', 'Japan', 123, true, false, '{"region": "Asia", "subregion": "Eastern Asia", "flag": "https://flagcdn.com/w320/jp.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c7a6a5e2-db69-e0a4-b886-781847eee203', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'israel', 'Israel', 10, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/il.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e5df2d37-ae30-c023-96fa-8d021529f25e', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'afghanistan', 'Afghanistan', 44, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Flag_of_the_Taliban.svg/320px-Flag_of_the_Taliban.svg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '71443e84-f9f7-b24b-74af-b3cf50e48a2c', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'cambodia', 'Cambodia', 18, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/kh.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5e2ed39f-e8ce-7a3c-9c7c-ae40657d9ad6', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'sri lanka', 'Sri Lanka', 22, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/lk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '6473ec53-c58f-ce05-4183-fa390b0d2177', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'turkmenistan', 'Turkmenistan', 7, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/tm.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e94226cd-61c6-afa2-8ca2-7cd2a50b1503', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'oman', 'Oman', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/om.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3e2ccd58-ef88-2542-aa36-b6b294b31846', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'azerbaijan', 'Azerbaijan', 10, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/az.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '01fc37a8-ab09-abd8-d0cd-6a4894b3b2fe', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'maldives', 'Maldives', 1, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/mv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e0580756-321d-ea35-b323-463715f37b7e', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'vietnam', 'Vietnam', 101, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/vn.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '9ef769f4-d38b-b841-e139-a35ced0b305c', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'singapore', 'Singapore', 6, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/sg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '279861b2-2b45-8718-10b2-5d0794c8f383', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'kazakhstan', 'Kazakhstan', 20, true, false, '{"region": "Asia", "subregion": "Central Asia", "flag": "https://flagcdn.com/w320/kz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'cb5e740f-ce7c-de44-c38c-8873f94d75dd', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'philippines', 'Philippines', 114, true, false, '{"region": "Asia", "subregion": "South-Eastern Asia", "flag": "https://flagcdn.com/w320/ph.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3fecce64-df38-9e44-f84d-af18ee541c6c', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'syria', 'Syria', 26, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/sy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '70e6567f-887e-bc9f-37d9-2c16611232b9', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'nepal', 'Nepal', 30, true, false, '{"region": "Asia", "subregion": "Southern Asia", "flag": "https://flagcdn.com/w320/np.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7c02efc0-61f4-c2ac-a424-7ef671b7268b', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'kuwait', 'Kuwait', 5, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/kw.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '12829c01-94d7-d7fc-a4c8-dd831eae0f15', '8a2b9539-5c83-ed99-181d-41fdb49365a7', 'united arab emirates', 'United Arab Emirates', 11, true, false, '{"region": "Asia", "subregion": "Western Asia", "flag": "https://flagcdn.com/w320/ae.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
-- 41 answers inserted for question 8a2b9539-5c83-ed99-181d-41fdb49365a7

-- Question: Name a country in Europe — its population in millions is your score
-- Region filter: Europe, 40 countries
-- Valid answers: 40, Score pool: 740, Viable: True
-- Zones: high=1, mid=7, checkout=32
UPDATE questions SET
  high_value_count = 1,
  mid_range_count = 7,
  checkout_count = 32,
  total_valid_count = 40,
  total_score_pool = 740,
  single_question_viable = true,
  status = 'active',
  difficulty_score = 0.00,
  updated_at = NOW()
WHERE id = '6406745b-9110-bac2-f207-1c8f84100558';

INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '11f4c11f-4b59-540e-0e4b-801b458fa121', '6406745b-9110-bac2-f207-1c8f84100558', 'slovenia', 'Slovenia', 2, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/si.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '14ef6449-fb09-2865-715e-d9996af324e0', '6406745b-9110-bac2-f207-1c8f84100558', 'sweden', 'Sweden', 11, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/se.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7b67c757-0ca4-6a35-327f-56a099286047', '6406745b-9110-bac2-f207-1c8f84100558', 'switzerland', 'Switzerland', 9, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/ch.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3754f110-9c8c-d026-5aeb-f8172e4652b2', '6406745b-9110-bac2-f207-1c8f84100558', 'poland', 'Poland', 37, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/pl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c2fc4933-1f09-f40e-de26-1c56db4f0f5a', '6406745b-9110-bac2-f207-1c8f84100558', 'france', 'France', 66, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/fr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '17b0e923-b33c-d8a1-06d3-98be876140c5', '6406745b-9110-bac2-f207-1c8f84100558', 'netherlands', 'Netherlands', 18, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/nl.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ef21002d-d442-4a4a-b07d-3c3679f78559', '6406745b-9110-bac2-f207-1c8f84100558', 'montenegro', 'Montenegro', 1, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/me.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c847e9b0-0d0b-32ac-cfaf-f4bbe97c1636', '6406745b-9110-bac2-f207-1c8f84100558', 'kosovo', 'Kosovo', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/xk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a6206173-3ce4-1f19-e5ba-7075eb2279ba', '6406745b-9110-bac2-f207-1c8f84100558', 'bulgaria', 'Bulgaria', 6, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/bg.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '4c3c218f-1038-3819-7548-d8520368f117', '6406745b-9110-bac2-f207-1c8f84100558', 'united kingdom', 'United Kingdom', 69, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/gb.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ace7fd87-c363-10a1-578e-a48108be735d', '6406745b-9110-bac2-f207-1c8f84100558', 'malta', 'Malta', 1, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/mt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'fdcbf308-be70-8876-3f62-fafe2ddef8ad', '6406745b-9110-bac2-f207-1c8f84100558', 'estonia', 'Estonia', 1, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/ee.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '3567a98a-dac1-8ee6-63fd-0f16015db6ed', '6406745b-9110-bac2-f207-1c8f84100558', 'denmark', 'Denmark', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/dk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '58caee29-f151-67e3-1e1e-46d7a04fc0af', '6406745b-9110-bac2-f207-1c8f84100558', 'greece', 'Greece', 10, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/gr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ed0025cb-deaf-47db-108c-6f4d73c7ce88', '6406745b-9110-bac2-f207-1c8f84100558', 'czechia', 'Czechia', 11, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/cz.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a75ad833-acb0-6148-a8b7-53ce0d4428c4', '6406745b-9110-bac2-f207-1c8f84100558', 'cyprus', 'Cyprus', 1, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/cy.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'b25e1146-036d-9425-4d2a-3805bc5d17b0', '6406745b-9110-bac2-f207-1c8f84100558', 'latvia', 'Latvia', 2, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/lv.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c9269696-eb2b-e16f-2569-61c3e4857845', '6406745b-9110-bac2-f207-1c8f84100558', 'germany', 'Germany', 83, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/de.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a2d8a30c-1abc-154e-2545-200f4e878fad', '6406745b-9110-bac2-f207-1c8f84100558', 'bosnia and herzegovina', 'Bosnia and Herzegovina', 3, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/ba.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '67547e3b-a05e-b73d-bf19-c185babe821f', '6406745b-9110-bac2-f207-1c8f84100558', 'italy', 'Italy', 59, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/it.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '50b054a3-39eb-fc1e-b606-bd964fa61592', '6406745b-9110-bac2-f207-1c8f84100558', 'romania', 'Romania', 19, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/ro.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '706f990d-eeee-a791-7bef-93bf80a559e4', '6406745b-9110-bac2-f207-1c8f84100558', 'ireland', 'Ireland', 5, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/ie.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1c42b8fe-b315-14c2-e2c2-4d8976d1fb14', '6406745b-9110-bac2-f207-1c8f84100558', 'luxembourg', 'Luxembourg', 1, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/lu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'c3374071-f597-bfd8-515f-10e18199cc38', '6406745b-9110-bac2-f207-1c8f84100558', 'austria', 'Austria', 9, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/at.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'aa758647-b4c5-2de3-5549-ce7506d6942b', '6406745b-9110-bac2-f207-1c8f84100558', 'portugal', 'Portugal', 11, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/pt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a1760bff-86f3-1626-28d6-7c9477fdf915', '6406745b-9110-bac2-f207-1c8f84100558', 'north macedonia', 'North Macedonia', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/mk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '03034c1d-3979-1cb3-f6ac-79ad25b50c47', '6406745b-9110-bac2-f207-1c8f84100558', 'serbia', 'Serbia', 7, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/rs.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '42d7f7c1-ccd1-4269-be73-90c824a841ad', '6406745b-9110-bac2-f207-1c8f84100558', 'moldova', 'Moldova', 3, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/md.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '7e163519-9860-6cda-cc24-02bea3e3f3c1', '6406745b-9110-bac2-f207-1c8f84100558', 'hungary', 'Hungary', 10, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/hu.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5151d932-d17c-b836-10b2-ea7db898ad32', '6406745b-9110-bac2-f207-1c8f84100558', 'lithuania', 'Lithuania', 3, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/lt.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'd37caefd-1244-217f-2a15-991b3d8dcf6c', '6406745b-9110-bac2-f207-1c8f84100558', 'norway', 'Norway', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/no.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'd09d0e76-f0c7-8774-9cdb-26319d09544c', '6406745b-9110-bac2-f207-1c8f84100558', 'albania', 'Albania', 2, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/al.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '77ed966a-2f12-9a6a-839f-6df1a24c40a4', '6406745b-9110-bac2-f207-1c8f84100558', 'slovakia', 'Slovakia', 5, true, false, '{"region": "Europe", "subregion": "Central Europe", "flag": "https://flagcdn.com/w320/sk.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '5d0454fe-b417-a276-4f0a-6eb757c5f760', '6406745b-9110-bac2-f207-1c8f84100558', 'belarus', 'Belarus', 9, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/by.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'a2567b0c-87cf-af66-01e9-f97d4e614604', '6406745b-9110-bac2-f207-1c8f84100558', 'belgium', 'Belgium', 12, true, false, '{"region": "Europe", "subregion": "Western Europe", "flag": "https://flagcdn.com/w320/be.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '1ca63df3-f2b3-ca79-29c9-56df604ebf2b', '6406745b-9110-bac2-f207-1c8f84100558', 'croatia', 'Croatia', 4, true, false, '{"region": "Europe", "subregion": "Southeast Europe", "flag": "https://flagcdn.com/w320/hr.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'ebd60d86-7abe-be7e-b314-36d12b35a031', '6406745b-9110-bac2-f207-1c8f84100558', 'finland', 'Finland', 6, true, false, '{"region": "Europe", "subregion": "Northern Europe", "flag": "https://flagcdn.com/w320/fi.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '42cffc59-22f4-2fcc-829b-1fa2501a89df', '6406745b-9110-bac2-f207-1c8f84100558', 'ukraine', 'Ukraine', 33, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/ua.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  '17a963dd-64fb-2b2e-e471-866c538790db', '6406745b-9110-bac2-f207-1c8f84100558', 'russia', 'Russia', 146, true, false, '{"region": "Europe", "subregion": "Eastern Europe", "flag": "https://flagcdn.com/w320/ru.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
INSERT INTO answers (id, question_id, answer_key, display_text, score, is_valid_darts, is_bust, metadata, materialized_at) VALUES (
  'e530fb91-1be2-960a-f20b-5bb9dd8a57fe', '6406745b-9110-bac2-f207-1c8f84100558', 'spain', 'Spain', 49, true, false, '{"region": "Europe", "subregion": "Southern Europe", "flag": "https://flagcdn.com/w320/es.png"}'::jsonb, NOW()
) ON CONFLICT (question_id, answer_key) DO NOTHING;
-- 40 answers inserted for question 6406745b-9110-bac2-f207-1c8f84100558
