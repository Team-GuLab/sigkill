export function extractDestination(message: string): string {
  const destMatch = message.match(/destination:(.*)\n/);
  const destination = destMatch ? destMatch[1].trim() : "";

  return destination;
}
