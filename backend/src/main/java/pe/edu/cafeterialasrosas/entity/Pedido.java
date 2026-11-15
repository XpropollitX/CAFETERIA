package pe.edu.cafeterialasrosas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "pedidos",
    indexes = {
        @Index(name = "idx_pedidos_id_usuario_fecha", columnList = "id_usuario, fecha_pedido"),
        @Index(name = "idx_pedidos_estado_fecha", columnList = "estado, fecha_pedido")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_pedido", nullable = false, updatable = false)
    private Instant fechaPedido = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "pedido")
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido")
    private List<Pago> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "pedido")
    private List<HistorialEstadoPedido> historialEstados = new ArrayList<>();
}
