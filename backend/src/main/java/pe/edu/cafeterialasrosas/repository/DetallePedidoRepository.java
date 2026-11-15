package pe.edu.cafeterialasrosas.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.cafeterialasrosas.entity.DetallePedido;

public interface DetallePedidoRepository extends JpaRepository<DetallePedido, Long> {

    List<DetallePedido> findByPedido_IdOrderByIdAsc(Long pedidoId);
}
