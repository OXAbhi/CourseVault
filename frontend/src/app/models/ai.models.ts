export interface AiStatusResponse {
  configured: boolean;
  model: string;
  baseUrl: string;
  keyPrefix: string;
}

export interface AiTestResponse {
  ok: boolean;
  model: string;
  reply: string;
}