// RFC 7807 shape returned by the backend's ApiExceptionHandler
// (backend/src/main/java/com/pecuni/common/exception/ApiExceptionHandler.java).
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  erros?: { campo: string; mensagem: string }[];
}
