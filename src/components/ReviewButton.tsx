import type { ReviewState } from '../lib/progress'
import { saveReviewState, useReviewState } from '../lib/progress'

type ReviewButtonProps = {
  itemId: string
  state: ReviewState
  itemType?: string
  workSlug?: string
  episode?: number
  payload?: Record<string, unknown>
  children: string
}

export function ReviewButton({ itemId, state, itemType, workSlug, episode, payload, children }: ReviewButtonProps) {
  const current = useReviewState(itemId)

  return (
    <button
      className={current === state ? 'icon-button selected' : 'icon-button secondary'}
      type="button"
      onClick={() => void saveReviewState(itemId, state, { itemType, workSlug, episode, payload })}
    >
      {children}
    </button>
  )
}
