import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="hero-panel rounded-4 p-4 p-lg-5 mb-5">
      <div class="row align-items-center g-4">
        <div class="col-lg-7">
          <p class="eyebrow mb-2">CAFÉ · POSTRES · MOMENTOS</p>
          <h1 class="display-4 fw-bold mb-3">Un momento especial, <span>hecho para ti.</span></h1>
          <p class="lead mb-4">Bienvenido a Cafetería Las Rosas. Explora nuestro menú y disfruta cada pausa.</p>
          <a class="btn btn-rose btn-lg" routerLink="/menu">Explorar el menú</a>
        </div>
        <div class="col-lg-5">
          <div class="coffee-art mx-auto" aria-hidden="true"><span>LR</span></div>
        </div>
      </div>
    </section>
    <section class="row g-3">
      <div class="col-md-4"><article class="feature-card"><span>01</span><h2>Elige</h2><p>Consulta productos y categorías disponibles.</p></article></div>
      <div class="col-md-4"><article class="feature-card"><span>02</span><h2>Ordena</h2><p>Arma tu carrito y confirma tu pedido en línea.</p></article></div>
      <div class="col-md-4"><article class="feature-card"><span>03</span><h2>Disfruta</h2><p>Registra tu pago y sigue el estado de tu pedido.</p></article></div>
    </section>
  `,
  styles: [`
    .hero-panel { background: linear-gradient(125deg, #442719, #7a452a 65%, #a96536); color: #fffaf5; overflow: hidden; }
    .hero-panel h1 span { color: #f4b77d; }
    .hero-panel .lead { color: #f2e3d6; max-width: 38rem; }
    .eyebrow { color: #f4b77d; font-size: .75rem; font-weight: 800; letter-spacing: .18em; }
    .btn-rose { background: #e6a16c; border-color: #e6a16c; color: #321b10; font-weight: 700; }
    .btn-rose:hover { background: #f0b37f; border-color: #f0b37f; color: #321b10; }
    .coffee-art { width: min(18rem, 70vw); aspect-ratio: 1; border-radius: 50%; display: grid; place-items: center; background: radial-gradient(circle at 50% 45%, #e8c39b 0 22%, #6a3a22 23% 29%, #f5e4d1 30% 42%, #d9945d 43% 46%, #5e3421 47% 60%, #382116 61%); box-shadow: 0 1.5rem 4rem #28160f66; }
    .coffee-art span { font-family: Georgia, serif; font-size: 2rem; color: #572e1b; border: 1px solid #8b451377; border-radius: 50%; width: 4.5rem; height: 4.5rem; display:grid;place-items:center; }
    .feature-card { height: 100%; padding: 1.5rem; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 1rem; }
    .feature-card span { color: var(--color-primary); font-weight: 800; letter-spacing: .12em; }
    .feature-card h2 { font-size: 1.3rem; margin-top: .8rem; }
    .feature-card p { color: #70645d; margin-bottom: 0; }
  `],
})
export class HomePageComponent {}
