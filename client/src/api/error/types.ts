export interface ErrorWebSocketMessage {
  type: "ERROR";
  code: string;
  message: string;
}
