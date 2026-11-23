// Production: by default the API is served from the same host under /api (reverse proxy).
// To call an API on another host, change this to a full URL, for example 'https://api.example.com/api'.
export const environment = {
  production: true,
  apiUrl: '/api',
};
