export interface ReviewSessionSummary {
  totalCount: number;
  pendingCount: number;
  completedCount: number;
}

export function buildReviewSessionSummary<T>(
  items: T[],
  isPending: (item: T) => boolean
): ReviewSessionSummary {
  const pendingCount = items.filter(isPending).length;
  return {
    totalCount: items.length,
    pendingCount,
    completedCount: items.length - pendingCount
  };
}

export function resolveCurrentSessionIndex<T>(
  items: T[],
  currentId: number | undefined,
  getId: (item: T) => number,
  isPending: (item: T) => boolean
) {
  if (!items.length) {
    return -1;
  }

  const firstPendingIndex = items.findIndex(isPending);
  const currentIndex = currentId === undefined ? -1 : items.findIndex((item) => getId(item) === currentId);

  if (currentIndex === -1) {
    return firstPendingIndex !== -1 ? firstPendingIndex : 0;
  }

  if (isPending(items[currentIndex])) {
    return currentIndex;
  }

  for (let index = currentIndex + 1; index < items.length; index++) {
    if (isPending(items[index])) {
      return index;
    }
  }

  return firstPendingIndex !== -1 ? firstPendingIndex : currentIndex;
}
