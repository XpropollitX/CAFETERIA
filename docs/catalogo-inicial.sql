-- Carga idempotente del catalogo adicional para Cafeteria Las Rosas.
-- Inserta solo categorias y productos que aun no existan; no elimina datos.
BEGIN;

INSERT INTO categorias (nombre, descripcion, activo)
SELECT datos.nombre, datos.descripcion, TRUE
FROM (
    VALUES
        ('Bebidas frías', 'Bebidas refrescantes preparadas al momento.'),
        ('Infusiones y tés', 'Tisanas, infusiones y bebidas calientes de la casa.'),
        ('Sándwiches artesanales', 'Sándwiches y panes calientes preparados al momento.'),
        ('Desayunos saludables', 'Opciones frescas para comenzar el día.'),
        ('Dulces para acompañar', 'Galletas y postres individuales para disfrutar con café.')
) AS datos(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias existente
    WHERE lower(existente.nombre) = lower(datos.nombre)
);

INSERT INTO productos (
    id_categoria,
    nombre,
    descripcion,
    precio,
    imagen_url,
    disponible
)
SELECT
    categoria.id_categoria,
    datos.nombre,
    datos.descripcion,
    datos.precio,
    datos.imagen_url,
    TRUE
FROM (
    VALUES
        (
            'Bebidas frías',
            'Cold brew de la casa',
            'Cafe infusionado en frio durante varias horas, servido con hielo.',
            10.50,
            '/uploads/product-images/dc5dde58-4928-4279-96c2-8cb48529c6d1.jpg'
        ),
        (
            'Bebidas frias',
            'Refresco cítrico de la casa',
            'Bebida helada con notas de limon y lima, ideal para refrescarse.',
            8.50,
            '/uploads/product-images/5545b8e1-c260-4cbb-a962-b8f46044976c.jpg'
        ),
        (
            'Infusiones y tés',
            'Infusion de hierbas',
            'Mezcla aromatica de hierbas seleccionadas, servida caliente.',
            7.00,
            '/uploads/product-images/a1633500-32da-4393-9795-96dac7190d76.jpg'
        ),
        (
            'Infusiones y tes',
            'Chai latte artesanal',
            'Te negro con especias, leche vaporizada y un toque de canela.',
            10.00,
            '/uploads/product-images/4df45958-ca96-403c-b903-60fad6786a3b.jpg'
        ),
        (
            'Sándwiches artesanales',
            'Panini de queso y tomate',
            'Pan tostado con queso fundido y tomate, preparado al momento.',
            13.50,
            '/uploads/product-images/a5ee0a7d-c5f3-4790-824c-eaa0db63bf71.jpg'
        ),
        (
            'Sandwiches artesanales',
            'Sándwich de pollo crocante',
            'Pollo crocante, vegetales frescos y salsa de la casa en pan suave.',
            15.00,
            '/uploads/product-images/25e938bc-1484-4296-9496-95b31a33de4d.jpg'
        ),
        (
            'Desayunos saludables',
            'Bowl verde con palta y huevo',
            'Bowl fresco de vegetales, palta y huevo con semillas crocantes.',
            14.00,
            '/uploads/product-images/4a190f2b-19d0-4cc8-a38a-c29ccc71c1a5.jpg'
        ),
        (
            'Desayunos saludables',
            'Tostadas francesas con frutos rojos',
            'Tostadas francesas doradas con fruta fresca y un toque de miel.',
            15.50,
            '/uploads/product-images/7f50d6b0-ce61-4ce0-97d7-9d65317e89f3.jpg'
        ),
        (
            'Dulces para acompañar',
            'Galleta de chocolate',
            'Galleta horneada con trozos generosos de chocolate.',
            5.00,
            '/uploads/product-images/e12beda9-0f38-48fe-8542-41db015bd631.jpg'
        ),
        (
            'Dulces para acompanar',
            'Brownie de cacao y nuez',
            'Brownie humedo de cacao con nueces y cobertura de chocolate.',
            6.50,
            '/uploads/product-images/4cc23ea9-855f-46c1-9634-77e4178f369d.jpg'
        )
) AS datos(categoria, nombre, descripcion, precio, imagen_url)
JOIN categorias categoria
    ON lower(categoria.nombre) = lower(datos.categoria)
WHERE categoria.activo = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM productos existente
      WHERE existente.id_categoria = categoria.id_categoria
        AND lower(existente.nombre) = lower(datos.nombre)
  );

COMMIT;

SELECT
    categoria.nombre AS categoria,
    count(producto.id_producto) AS productos,
    count(producto.imagen_url) AS productos_con_imagen
FROM categorias categoria
LEFT JOIN productos producto
    ON producto.id_categoria = categoria.id_categoria
WHERE lower(categoria.nombre) IN (
    'bebidas frías',
    'infusiones y tés',
    'sándwiches artesanales',
    'desayunos saludables',
    'dulces para acompañar'
)
GROUP BY categoria.id_categoria, categoria.nombre
ORDER BY categoria.nombre;
