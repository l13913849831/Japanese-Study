import { create } from "zustand";

interface UiStore {
  currentPlanId?: number;
  setCurrentPlanId: (planId?: number) => void;
}

export const useUiStore = create<UiStore>((set) => ({
  currentPlanId: undefined,
  setCurrentPlanId: (currentPlanId) => set({ currentPlanId })
}));
