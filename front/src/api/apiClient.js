const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

async function request(method, path, body) {
  const opts = {
    method,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
  };
  if (body !== undefined) opts.body = JSON.stringify(body);

  const res = await fetch(`${API_BASE}${path}`, opts);
  let data = null;
  const text = await res.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const err = new Error((data && data.message) || res.statusText);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
};

export function poll(fetcher, cb, intervalMs = 5000) {
  let stopped = false;
  const tick = async () => {
    if (stopped) return;
    try {
      const data = await fetcher();
      if (!stopped) cb(data);
    } catch {
      /* ignore transient poll errors */
    }
  };
  tick();
  const id = setInterval(tick, intervalMs);
  return () => {
    stopped = true;
    clearInterval(id);
  };
}
