export const statusColors: Record<string, string> = {
  AVAILABLE: '#16c063', RESERVED: '#f59e0b', CHARGING: '#0ea5e9', OUT_OF_SERVICE: '#ef4444', MAINTENANCE: '#8b5cf6',
};
export const palette = ['#0a9e4f', '#252a33', '#3ed882', '#677084', '#7cebab', '#b4bbc6', '#0c7c41', '#8a93a3'];
export const axis = { fontSize: 12, fill: '#677084' };
export const toSeries = (record: Record<string, number>) => Object.entries(record).map(([name, value]) => ({ name, value: Number(value) }));
