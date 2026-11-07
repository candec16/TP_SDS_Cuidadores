import React from 'react';

export default function Navbar({ user, ownProfile, onOpenAuth, onOpenProfileForm, onLogout }) {
  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <div className="brand">
          <div className="brand-icon" aria-hidden="true">RC</div>
          <div>
            <h1 className="brand-title">RedCuidar Córdoba</h1>
            <p className="brand-subtitle">Directorio de cuidadores de personas mayores</p>
          </div>
        </div>

        <nav className="nav-actions">
          {!user ? (
            <>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => onOpenAuth('login')}
              >
                Iniciar sesión
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => onOpenAuth('register')}
              >
                Registrarme como cuidador/a
              </button>
            </>
          ) : (
            <>
              <span className="user-pill" title="Cuenta autenticada mediante JWT">
                <span className="status-dot" />
                {user.email}
              </span>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => onOpenProfileForm(ownProfile || null)}
              >
                {ownProfile ? 'Editar mi perfil' : 'Crear mi perfil'}
              </button>
              <button
                type="button"
                className="btn btn-outline"
                onClick={onLogout}
              >
                Salir
              </button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}

