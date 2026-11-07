import React from 'react';

export default function ProfileCard({ profile, isOwner, onEdit }) {
  const initials = `${profile.nombre?.[0] || ''}${profile.apellido?.[0] || ''}`.toUpperCase();

  return (
    <article className={`profile-card ${isOwner ? 'profile-card-own' : ''}`}>
      <div className="profile-card-header">
        <div className="profile-identity">
          <div className="avatar" aria-hidden="true">
            {initials}
          </div>
          <div>
            <div className="profile-name-row">
              <h3 className="profile-name">
                {profile.nombre} {profile.apellido}
              </h3>
              <span className="profile-id-tag" title="Identificador del recurso en la API">
                ID #{profile.id}
              </span>
            </div>
            <span className="zone-badge">{profile.zona}</span>
          </div>
        </div>

        {isOwner && <span className="owner-tag">Tu perfil</span>}
      </div>

      <p className="profile-description">{profile.descripcion}</p>

      <div className="profile-details">
        {profile.estudios && (
          <div className="detail-block">
            <span className="detail-label">Estudios y formación</span>
            <p>{profile.estudios}</p>
          </div>
        )}

        {profile.intereses && (
          <div className="detail-block">
            <span className="detail-label">Intereses y actividades</span>
            <p>{profile.intereses}</p>
          </div>
        )}
      </div>

      <div className="profile-card-footer">
        <span className="contact-email" title="Correo de contacto profesional">
          Contacto: {profile.userEmail}
        </span>

        {/*
          Nota de Seguridad (Sección 5.2 de la Propuesta - IDOR CWE-639):
          La interfaz gráfica oculta el botón "Editar" en perfiles de terceros,
          mostrándolo solo si el perfil pertenece al usuario autenticado (isOwner).
          Sin embargo, ocultar controles en el cliente NO reemplaza la autorización en el servidor.
        */}
        {isOwner && (
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => onEdit(profile)}
          >
            Editar perfil
          </button>
        )}
      </div>
    </article>
  );
}

