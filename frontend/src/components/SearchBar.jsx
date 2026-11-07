import React from 'react';

const ZONAS_SUGERIDAS = [
  'Nueva Córdoba',
  'Centro',
  'Alberdi',
  'Cerro de las Rosas',
  'General Paz',
  'Alta Córdoba',
  'Güemes',
  'Observatorio',
  'Jardín'
];

const CHIPS_RAPIDOS = ['Nueva Córdoba', 'Alberdi', 'Cerro de las Rosas', 'Centro', 'General Paz'];

export default function SearchBar({ zonaInput, setZonaInput, activeZona, onSearch, onClear, totalResults }) {
  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch(zonaInput);
  };

  const handleChipClick = (zona) => {
    setZonaInput(zona);
    onSearch(zona);
  };

  return (
    <section className="search-section">
      <div className="search-card">
        <div className="search-header">
          <h2>Encontrá profesionales del cuidado en tu zona</h2>
          <p>
            Consultá perfiles públicos sin necesidad de registrarte. Podés elegir un barrio sugerido o escribir cualquier zona.
          </p>
        </div>

        <form className="search-form" onSubmit={handleSubmit}>
          <div className="search-input-wrapper">
            <input
              type="text"
              list="zonas-cordoba"
              className="search-input"
              placeholder="Ej.: Nueva Córdoba, Alberdi, Centro..."
              value={zonaInput}
              onChange={(e) => setZonaInput(e.target.value)}
              aria-label="Buscar por zona de residencia"
            />
            <datalist id="zonas-cordoba">
              {ZONAS_SUGERIDAS.map((z) => (
                <option key={z} value={z} />
              ))}
            </datalist>
          </div>

          <button type="submit" className="btn btn-primary">
            Buscar por zona
          </button>

          {(zonaInput || activeZona) && (
            <button type="button" className="btn btn-outline" onClick={onClear}>
              Ver todos
            </button>
          )}
        </form>

        <div className="quick-chips">
          <span className="chips-label">Zonas frecuentes:</span>
          {CHIPS_RAPIDOS.map((z) => (
            <button
              key={z}
              type="button"
              className={`chip ${activeZona === z ? 'chip-active' : ''}`}
              onClick={() => handleChipClick(z)}
            >
              {z}
            </button>
          ))}
        </div>

        {activeZona && (
          <div className="active-filter-banner">
            <span>
              Filtro aplicado: <strong>{activeZona}</strong> — {totalResults}{' '}
              {totalResults === 1 ? 'perfil encontrado' : 'perfiles encontrados'}
            </span>
          </div>
        )}
      </div>
    </section>
  );
}

