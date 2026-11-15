package pe.edu.cafeterialasrosas.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.cafeterialasrosas.entity.EstadoPago;
import pe.edu.cafeterialasrosas.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByPedido_IdOrderByIdAsc(Long pedidoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pago from Pago pago where pago.id = :pagoId")
    Optional<Pago> findByIdForUpdate(@Param("pagoId") Long pagoId);

    @Query("""
        select coalesce(sum(pago.monto), 0)
        from Pago pago
        where pago.pedido.id = :pedidoId and pago.estadoPago = :estado
        """)
    BigDecimal sumMontoByPedidoAndEstado(
        @Param("pedidoId") Long pedidoId,
        @Param("estado") EstadoPago estado
    );
}
