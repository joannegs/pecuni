// Shapes mirror the backend's request/response DTOs verbatim
// (backend/src/main/java/com/pecuni/auth/dto/*.java) — including the
// Portuguese field names (nome, senha) the DTOs actually use.

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface GoogleLoginRequest {
  idToken: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}
