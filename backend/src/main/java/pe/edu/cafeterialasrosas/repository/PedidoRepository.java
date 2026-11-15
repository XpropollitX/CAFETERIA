package pe.edu.cafeterialasrosas.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cafeterialasrosas.entity.EstadoPedido;
import pe.edu.cafeterialasrosas.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuario_IdOrderByFechaPedidoDesc(Long usuarioId);

    List<Pedido> findByEstadoOrderByFechaPedidoAsc(EstadoPedido estado);

    List<Pedido> findByEstadoInOrderByFechaPedidoAsc(List<EstadoPedido> estados);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :pedidoId")
    Optional<Pedido> findByIdForUpdate(@Param("pedidoId") Long pedidoId);
}
