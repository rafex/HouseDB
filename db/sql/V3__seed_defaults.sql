BEGIN;

INSERT INTO categories (category_id, name, description)
VALUES
  (gen_random_uuid(), 'camping', 'Objetos para campamento y actividades al aire libre'),
  (gen_random_uuid(), 'cocina', 'Utensilios y equipo de cocina'),
  (gen_random_uuid(), 'herramientas', 'Herramientas para mantenimiento del hogar'),
  (gen_random_uuid(), 'limpieza', 'Productos y herramientas de limpieza')
ON CONFLICT (name) DO NOTHING;

COMMIT;
