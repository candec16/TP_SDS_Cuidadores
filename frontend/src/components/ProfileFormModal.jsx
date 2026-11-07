import React, { useState } from 'react';
import { createProfile, updateProfile } from '../services/api';

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

export default function ProfileFormModal({ existingProfile, token, onClose, onSaved }) {
  const isEditing = Boolean(existingProfile);

  const [nombre, setNombre] = useState(existingProfile?.nombre || '');
  const [apellido, setApellido] = useState(existingProfile?.apellido || '');
  const [zona, setZona] = useState(existingProfile?.zona || '');
  const [descripcion, setDescripcion] = useState(existingProfile?.descripcion || '');
  const [estudios, setEstudios] = useState(existingProfile?.estudios || '');
  const [intereses, setIntereses] = useState(existingProfile?.intereses || '');

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    const payload = {
      nombre,
      apellido,
      zona,
      descripcion,
      estudios,
      intereses
    };

    try {
      const saved = isEditing
        ? await updateProfile(existingProfile.id, payload, token)
        : await createProfile(payload, token);

      onSaved(saved);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-content modal-lg" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{isEditing ? 'Editar mi perfil profesional' : 'Completar mi perfil de cuidador/a'}</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Cerrar">
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-form">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="prof-nombre">Nombre *</label>
              <input
                id="prof-nombre"
                type="text"
                required
                maxLength={100}
                placeholder="Ej.: María"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
              />
            </div>

            <div className="form-group">
              <label htmlFor="prof-apellido">Apellido *</label>
              <input
                id="prof-apellido"
                type="text"
                required
                maxLength={100}
                placeholder="Ej.: González"
                value={apellido}
                onChange={(e) => setApellido(e.target.value)}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="prof-zona">Zona de residencia / cobertura *</label>
            <input
              id="prof-zona"
              type="text"
              list="zonas-form-cordoba"
              required
              maxLength={120}
              placeholder="Ej.: Nueva Córdoba, Alberdi, Centro..."
              value={zona}
              onChange={(e) => setZona(e.target.value)}
            />
            <datalist id="zonas-form-cordoba">
              {ZONAS_SUGERIDAS.map((z) => (
                <option key={z} value={z} />
              ))}
            </datalist>
          </div>

          <div className="form-group">
            <label htmlFor="prof-descripcion">Descripción profesional y experiencia *</label>
            <textarea
              id="prof-descripcion"
              rows={4}
              required
              maxLength={1000}
              placeholder="Contá tu experiencia acompañando adultos mayores, disponibilidad horaria y tareas que realizás..."
              value={descripcion}
              onChange={(e) => setDescripcion(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label htmlFor="prof-estudios">Estudios y certificaciones</label>
            <input
              id="prof-estudios"
              type="text"
              maxLength={500}
              placeholder="Ej.: Auxiliar Gerontológico, Enfermería, Primeros Auxilios..."
              value={estudios}
              onChange={(e) => setEstudios(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label htmlFor="prof-intereses">Intereses y actividades recreativas</label>
            <input
              id="prof-intereses"
              type="text"
              maxLength={500}
              placeholder="Ej.: Lectura, caminatas, juegos de mesa, estimulación cognitiva..."
              value={intereses}
              onChange={(e) => setIntereses(e.target.value)}
            />
          </div>

          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Guardando...' : isEditing ? 'Guardar cambios' : 'Publicar perfil'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

