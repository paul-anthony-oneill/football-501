-- ============================================================
-- DEV SEED: footballer entities for autocomplete testing
-- ============================================================
-- Run this manually against a local dev database to populate
-- the entities table so the autocomplete dropdown has data.
--
-- Usage:
--   psql -U football501 -d football501 -f seed_entities_dev.sql
--
-- This is NOT a Flyway migration — do not commit to production.
-- Production entities are populated automatically by
-- AdminAnswerService → EntitySearchService.upsertEntity()
-- when answers are bulk-imported.
-- ============================================================

INSERT INTO entities (entity_type, display_name, normalized_name, hint)
VALUES
  -- Premier League legends
  ('footballer', 'Sergio Agüero',        'sergio aguero',        'AR'),
  ('footballer', 'Wayne Rooney',         'wayne rooney',         'GB-ENG'),
  ('footballer', 'Thierry Henry',        'thierry henry',        'FR'),
  ('footballer', 'Frank Lampard',        'frank lampard',        'GB-ENG'),
  ('footballer', 'Steven Gerrard',       'steven gerrard',       'GB-ENG'),
  ('footballer', 'Patrick Vieira',       'patrick vieira',       'FR'),
  ('footballer', 'Dennis Bergkamp',      'dennis bergkamp',      'NL'),
  ('footballer', 'Peter Schmeichel',     'peter schmeichel',     'DK'),
  ('footballer', 'Roy Keane',            'roy keane',            'IE'),
  ('footballer', 'Eric Cantona',         'eric cantona',         'FR'),
  ('footballer', 'Alan Shearer',         'alan shearer',         'GB-ENG'),
  ('footballer', 'Michael Owen',         'michael owen',         'GB-ENG'),
  ('footballer', 'Ryan Giggs',           'ryan giggs',           'GB-WAL'),
  ('footballer', 'Paul Scholes',         'paul scholes',         'GB-ENG'),
  ('footballer', 'Gary Neville',         'gary neville',         'GB-ENG'),
  ('footballer', 'Didier Drogba',        'didier drogba',        'CI'),
  ('footballer', 'John Terry',           'john terry',           'GB-ENG'),
  ('footballer', 'Ashley Cole',          'ashley cole',          'GB-ENG'),
  ('footballer', 'Joe Hart',             'joe hart',             'GB-ENG'),
  ('footballer', 'Robin van Persie',     'robin van persie',     'NL'),
  ('footballer', 'Cesc Fàbregas',        'cesc fabregas',        'ES'),
  ('footballer', 'Yaya Touré',           'yaya toure',           'CI'),
  ('footballer', 'Kompany Vincent',      'kompany vincent',      'BE'),
  ('footballer', 'Vincent Kompany',      'vincent kompany',      'BE'),
  ('footballer', 'David Silva',          'david silva',          'ES'),
  ('footballer', 'Kevin De Bruyne',      'kevin de bruyne',      'BE'),
  ('footballer', 'Mohamed Salah',        'mohamed salah',        'EG'),
  ('footballer', 'Sadio Mané',           'sadio mane',           'SN'),
  ('footballer', 'Roberto Firmino',      'roberto firmino',      'BR'),
  ('footballer', 'Virgil van Dijk',      'virgil van dijk',      'NL'),
  ('footballer', 'Alisson Becker',       'alisson becker',       'BR'),
  ('footballer', 'Trent Alexander-Arnold','trent alexander-arnold','GB-ENG'),
  ('footballer', 'Jordan Henderson',     'jordan henderson',     'GB-ENG'),
  ('footballer', 'Harry Kane',           'harry kane',           'GB-ENG'),
  ('footballer', 'Son Heung-min',        'son heung-min',        'KR'),
  ('footballer', 'Hugo Lloris',          'hugo lloris',          'FR'),
  ('footballer', 'Dele Alli',            'dele alli',            'GB-ENG'),
  ('footballer', 'Christian Eriksen',    'christian eriksen',    'DK'),
  ('footballer', 'Jan Vertonghen',       'jan vertonghen',       'BE'),
  ('footballer', 'Toby Alderweireld',    'toby alderweireld',    'BE'),
  ('footballer', 'N''Golo Kanté',        'ngolo kante',          'FR'),
  ('footballer', 'Eden Hazard',          'eden hazard',          'BE'),
  ('footballer', 'Willian',              'willian',              'BR'),
  ('footballer', 'Pedro Rodríguez',      'pedro rodriguez',      'ES'),
  ('footballer', 'Petr Čech',            'petr cech',            'CZ'),
  ('footballer', 'Romelu Lukaku',        'romelu lukaku',        'BE'),
  ('footballer', 'Marcus Rashford',      'marcus rashford',      'GB-ENG'),
  ('footballer', 'Anthony Martial',      'anthony martial',      'FR'),
  ('footballer', 'Paul Pogba',           'paul pogba',           'FR'),
  ('footballer', 'Bruno Fernandes',      'bruno fernandes',      'PT'),
  ('footballer', 'Luke Shaw',            'luke shaw',            'GB-ENG'),
  ('footballer', 'Aaron Wan-Bissaka',    'aaron wan-bissaka',    'GB-ENG'),
  ('footballer', 'David de Gea',         'david de gea',         'ES'),
  ('footballer', 'Ederson',              'ederson',              'BR'),
  ('footballer', 'Raheem Sterling',      'raheem sterling',      'GB-ENG'),
  ('footballer', 'Leroy Sané',           'leroy sane',           'DE'),
  ('footballer', 'Gabriel Jesus',        'gabriel jesus',        'BR'),
  ('footballer', 'Bernardo Silva',       'bernardo silva',       'PT'),
  ('footballer', 'Riyad Mahrez',         'riyad mahrez',         'DZ'),
  ('footballer', 'Ilkay Gündogan',       'ilkay gundogan',       'DE'),
  ('footballer', 'Phil Foden',           'phil foden',           'GB-ENG'),
  ('footballer', 'Erling Haaland',       'erling haaland',       'NO'),
  ('footballer', 'Bukayo Saka',          'bukayo saka',          'GB-ENG'),
  ('footballer', 'Martin Ødegaard',      'martin odegaard',      'NO'),
  ('footballer', 'Gabriel Martinelli',   'gabriel martinelli',   'BR'),
  ('footballer', 'Declan Rice',          'declan rice',          'GB-ENG'),
  ('footballer', 'Kai Havertz',          'kai havertz',          'DE'),
  ('footballer', 'Granit Xhaka',         'granit xhaka',         'CH'),
  ('footballer', 'Oleksandr Zinchenko',  'oleksandr zinchenko',  'UA'),
  ('footballer', 'Ben White',            'ben white',            'GB-ENG'),
  ('footballer', 'Gabriel Magalhães',    'gabriel magalhaes',    'BR'),
  ('footballer', 'William Saliba',       'william saliba',       'FR'),
  ('footballer', 'Emile Smith Rowe',     'emile smith rowe',     'GB-ENG'),
  -- Liverpool current
  ('footballer', 'Darwin Núñez',         'darwin nunez',         'UY'),
  ('footballer', 'Dominik Szoboszlai',   'dominik szoboszlai',   'HU'),
  ('footballer', 'Alexis Mac Allister',  'alexis mac allister',  'AR'),
  ('footballer', 'Luis Díaz',            'luis diaz',            'CO'),
  ('footballer', 'Cody Gakpo',           'cody gakpo',           'NL'),
  ('footballer', 'Diogo Jota',           'diogo jota',           'PT'),
  -- Chelsea
  ('footballer', 'Cole Palmer',          'cole palmer',          'GB-ENG'),
  ('footballer', 'Nicolas Jackson',      'nicolas jackson',      'SN'),
  ('footballer', 'Enzo Fernández',       'enzo fernandez',       'AR'),
  ('footballer', 'Mykhailo Mudryk',      'mykhailo mudryk',      'UA'),
  ('footballer', 'Reece James',          'reece james',          'GB-ENG'),
  ('footballer', 'Ben Chilwell',         'ben chilwell',         'GB-ENG'),
  -- Man Utd
  ('footballer', 'Rasmus Højlund',       'rasmus hojlund',       'DK'),
  ('footballer', 'Alejandro Garnacho',   'alejandro garnacho',   'AR'),
  ('footballer', 'Kobbie Mainoo',        'kobbie mainoo',        'GB-ENG'),
  ('footballer', 'André Onana',          'andre onana',          'CM'),
  -- Newcastle
  ('footballer', 'Alexander Isak',       'alexander isak',       'SE'),
  ('footballer', 'Bruno Guimarães',      'bruno guimaraes',      'BR'),
  ('footballer', 'Nick Pope',            'nick pope',            'GB-ENG'),
  -- Aston Villa
  ('footballer', 'Ollie Watkins',        'ollie watkins',        'GB-ENG'),
  ('footballer', 'Emiliano Martínez',    'emiliano martinez',    'AR'),
  ('footballer', 'Youri Tielemans',      'youri tielemans',      'BE'),
  ('footballer', 'Pau Torres',           'pau torres',           'ES')
ON CONFLICT (entity_type, normalized_name) DO NOTHING;
