// The backend stores and exchanges money as integer cents (e.g. 64536 is 645.36). Formatting
// and parsing to a human-facing amount is the client's job, kept in one place here.

/** Cents → a fixed two-decimal string for display. */
export const formatMoney = (cents: number): string => (cents / 100).toFixed(2);

/** Cents → a whole-currency number, for pre-filling an editable amount field. */
export const toMajorUnits = (cents: number): number => cents / 100;

/** A whole-currency amount from an input field → integer cents for the wire. */
export const toCents = (majorUnits: number): number => Math.round(majorUnits * 100);
