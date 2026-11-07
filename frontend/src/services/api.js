/**
 * Servicio centralizado de comunicación HTTP con la API REST (Spring Boot - Puerto 8080).
 * Gestiona el envío de JSON y la cabecera Authorization: Bearer <JWT>.
 */
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

async function handleResponse(response) {
  const text = await response.text();
  let data = null;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }

  if (!response.ok) {
    const errorMessage =
      (data && data.error) ||
      `Error HTTP ${response.status}: No se pudo completar la operación en el servidor.`;
    throw new Error(errorMessage);
  }

  return data;
}

/**
 * Consulta pública de perfiles (con filtro opcional por zona).
 * No requiere token JWT.
 */
export async function fetchProfiles(zona = '') {
  const trimmed = zona.trim();
  const url = trimmed
    ? `${API_BASE_URL}/profiles?zona=${encodeURIComponent(trimmed)}`
    : `${API_BASE_URL}/profiles`;

  const response = await fetch(url);
  return handleResponse(response);
}

/**
 * Registro de nueva cuenta de usuario (POST /api/auth/register).
 */
export async function registerUser(email, password) {
  const response = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  return handleResponse(response);
}

/**
 * Inicio de sesión (POST /api/auth/login).
 */
export async function loginUser(email, password) {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  return handleResponse(response);
}

/**
 * Creación del perfil propio del usuario autenticado (POST /api/profiles).
 */
export async function createProfile(profileData, token) {
  const response = await fetch(`${API_BASE_URL}/profiles`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(profileData)
  });
  return handleResponse(response);
}

/**
 * Edición de perfil por ID (PUT /api/profiles/{id}).
 */
export async function updateProfile(id, profileData, token) {
  const response = await fetch(`${API_BASE_URL}/profiles/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify(profileData)
  });
  return handleResponse(response);
}

