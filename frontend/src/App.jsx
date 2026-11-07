import React, { useEffect, useState, useCallback } from 'react';
import Navbar from './components/Navbar';
import SearchBar from './components/SearchBar';
import ProfileCard from './components/ProfileCard';
import AuthModal from './components/AuthModal';
import ProfileFormModal from './components/ProfileFormModal';
import { fetchProfiles } from './services/api';

const AUTH_STORAGE_KEY = 'cuidadores_auth';

export default function App() {
  const [user, setUser] = useState(() => {
    try {
      const saved = localStorage.getItem(AUTH_STORAGE_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  const [profiles, setProfiles] = useState([]);
  const [ownProfile, setOwnProfile] = useState(null);
  const [zonaInput, setZonaInput] = useState('');
  const [activeZona, setActiveZona] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successBanner, setSuccessBanner] = useState('');

  // Modales
  const [authModalMode, setAuthModalMode] = useState(null); // 'login' | 'register' | null
  const [profileModalData, setProfileModalData] = useState(undefined); // undefined = cerrado, null = crear, objeto = editar

  // Carga la lista pública de perfiles (con o sin filtro de zona)
  const loadProfiles = useCallback(async (zonaFiltro = '') => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchProfiles(zonaFiltro);
      setProfiles(data);
    } catch (err) {
      setProfiles([]);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  // Sincroniza cuál es el perfil propio del usuario logueado (independientemente del filtro activo)
  const syncOwnProfile = useCallback(async (currentUser) => {
    if (!currentUser) {
      setOwnProfile(null);
      return;
    }
    try {
      const all = await fetchProfiles('');
      const found = all.find(
        (p) => p.userEmail.toLowerCase() === currentUser.email.toLowerCase()
      );
      setOwnProfile(found || null);
    } catch {
      // Si el backend no responde aún, loadProfiles ya mostrará el aviso principal
    }
  }, []);

  useEffect(() => {
    loadProfiles('');
  }, [loadProfiles]);

  useEffect(() => {
    syncOwnProfile(user);
  }, [user, syncOwnProfile]);

  const handleSearch = (zona) => {
    const trimmed = zona.trim();
    setActiveZona(trimmed);
    loadProfiles(trimmed);
  };

  const handleClearSearch = () => {
    setZonaInput('');
    setActiveZona('');
    loadProfiles('');
  };

  const handleAuthSuccess = (authData, isNewRegistration) => {
    setUser(authData);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(authData));
    setAuthModalMode(null);

    if (isNewRegistration) {
      setSuccessBanner('¡Cuenta creada con éxito! Completá tu perfil profesional para aparecer en las búsquedas.');
      setProfileModalData(null); // Abre directamente el alta de perfil
    } else {
      setSuccessBanner(`Bienvenido/a nuevamente, ${authData.email}`);
    }
  };

  const handleLogout = () => {
    setUser(null);
    setOwnProfile(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setSuccessBanner('Cerraste sesión correctamente.');
  };

  const handleProfileSaved = (savedProfile) => {
    setOwnProfile(savedProfile);
    setProfileModalData(undefined);
    setSuccessBanner('Tu perfil profesional fue guardado y actualizado en el directorio público.');
    loadProfiles(activeZona);
  };

  return (
    <div className="app-layout">
      <Navbar
        user={user}
        ownProfile={ownProfile}
        onOpenAuth={(mode) => {
          setSuccessBanner('');
          setAuthModalMode(mode);
        }}
        onOpenProfileForm={(profileToEdit) => {
          setSuccessBanner('');
          setProfileModalData(profileToEdit);
        }}
        onLogout={handleLogout}
      />

      <main className="container main-content">
        {successBanner && (
          <div className="alert alert-success">
            <span>{successBanner}</span>
            <button
              type="button"
              className="alert-dismiss"
              onClick={() => setSuccessBanner('')}
              aria-label="Cerrar aviso"
            >
              ×
            </button>
          </div>
        )}

        {user && !ownProfile && !loading && !error && (
          <div className="cta-banner">
            <div>
              <strong>Todavía no publicaste tu perfil profesional.</strong>
              <p>Completá tus datos, zona de residencia, estudios e intereses para que las familias puedan encontrarte.</p>
            </div>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setProfileModalData(null)}
            >
              Completar mi perfil ahora
            </button>
          </div>
        )}

        <SearchBar
          zonaInput={zonaInput}
          setZonaInput={setZonaInput}
          activeZona={activeZona}
          onSearch={handleSearch}
          onClear={handleClearSearch}
          totalResults={profiles.length}
        />

        {error && (
          <div className="alert alert-error">
            <div>
              <strong>Error al consultar la API:</strong> {error}
            </div>
          </div>
        )}

        {loading ? (
          <div className="empty-state">
            <p>Cargando perfiles de cuidadores...</p>
          </div>
        ) : !error && profiles.length === 0 ? (
          <div className="empty-state">
            <h3>No se encontraron cuidadores en esa zona</h3>
            <p>
              Probá buscando otra zona de Córdoba o hacé clic en{' '}
              <button type="button" className="link-button" onClick={handleClearSearch}>
                ver todos los perfiles disponibles
              </button>
              .
            </p>
          </div>
        ) : (
          <section className="profiles-grid" aria-label="Listado de cuidadores">
            {profiles.map((profile) => {
              const isOwner =
                Boolean(user) &&
                profile.userEmail.toLowerCase() === user.email.toLowerCase();

              return (
                <ProfileCard
                  key={profile.id}
                  profile={profile}
                  isOwner={isOwner}
                  onEdit={(prof) => setProfileModalData(prof)}
                />
              );
            })}
          </section>
        )}
      </main>

      <footer className="footer">
        <div className="container footer-inner">
          <span>RedCuidar Córdoba — Trabajo Práctico Integrador · Seguridad en el Desarrollo de Software (UTN FRC - 4K2A)</span>
          <span>Entorno académico local (sin datos reales)</span>
        </div>
      </footer>

      {authModalMode && (
        <AuthModal
          initialMode={authModalMode}
          onClose={() => setAuthModalMode(null)}
          onAuthSuccess={handleAuthSuccess}
        />
      )}

      {profileModalData !== undefined && user && (
        <ProfileFormModal
          existingProfile={profileModalData}
          token={user.token}
          onClose={() => setProfileModalData(undefined)}
          onSaved={handleProfileSaved}
        />
      )}
    </div>
  );
}

