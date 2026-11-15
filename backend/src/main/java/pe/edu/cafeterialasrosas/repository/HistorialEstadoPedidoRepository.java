package pe.edu.cafeterialasrosas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cafeterialasrosas.entity.HistorialEstadoPedido;

public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedido_IdOrderByFechaCambioAscIdAsc(Long pedidoId);
}
