package pe.edu.cafeterialasrosas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cafeterialasrosas.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByDisponibleTrueAndCategoria_ActivoTrueOrderByNombreAsc();

    List<Producto> findByCategoria_IdOrderByNombreAsc(Long categoriaId);

    List<Producto> findByCategoria_IdAndDisponibleTrueOrderByNombreAsc(Long categoriaId);
}
