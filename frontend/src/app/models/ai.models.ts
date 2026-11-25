export interface AiStatusResponse {
  configured: boolean;
  model: string;
  baseUrl: string;
}

export interface AiTestResponse {
  ok: boolean;
  model: string;
  reply: string;
}