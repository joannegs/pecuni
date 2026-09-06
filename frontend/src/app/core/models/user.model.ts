export type AuthProvider = 'LOCAL' | 'GOOGLE';

// Field names mirror the backend's UserResponse DTO (com.pecuni.auth.dto)
// verbatim — nome/email are Portuguese there, unlike the rest of the DTOs.
export interface User {
  id: string;
  nome: string;
  email: string;
  provider: AuthProvider;
}
