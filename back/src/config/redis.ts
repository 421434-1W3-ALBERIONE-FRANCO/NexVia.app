import Redis from 'ioredis';
import { config } from './env';

export let redis: Redis | null = null;

export function initRedis() {
  if (!config.REDIS_HOST) {
    console.warn('⚠️  Redis not configured. Sessions and rate limiting will be in-memory only.');
    return null;
  }

  redis = new Redis({
    host: config.REDIS_HOST,
    port: config.REDIS_PORT || 6379,
    password: config.REDIS_PASSWORD || undefined,
    retryStrategy: (times) => {
      const delay = Math.min(times * 50, 2000);
      return delay;
    },
    reconnectOnError: (err) => {
      const targetError = 'READONLY';
      if (err.message.includes(targetError)) {
        return true;
      }
      return false;
    },
  });

  redis.on('connect', () => {
    console.log('✅ Redis connected');
  });

  redis.on('error', (err) => {
    console.error('❌ Redis error:', err);
  });

  return redis;
}

export async function closeRedis() {
  if (redis) {
    await redis.quit();
  }
}
