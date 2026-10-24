export type Money = { amount: string; currency: string };
export type User = { id: string; name: string; email: string; role: "CUSTOMER" | "ADMIN"; createdAt: string };
export type Page<T> = { items: T[]; page: number; size: number; totalElements: number; totalPages: number };
export type Category = { id: string; name: string; slug: string; active: boolean; version: number };
export type Availability = { availableQuantity: number; availableForPurchase: boolean };
export type Presentation = {
  id: string; productId: string; sku: string; volume: number; unit: string; price: Money; currency: string;
  active: boolean; version: number; availability?: Availability;
};
export type Product = {
  id: string; slug: string; name: string; brand: string; description?: string; category: Category;
  active: boolean; published?: boolean; publishable?: boolean; version: number; presentations: Presentation[];
  images: { id: string; url: string; altText: string; position: number; primary: boolean; contentType: string }[];
};
export type CartItem = {
  id: string; presentationId: string; productId: string; productName: string; productSlug: string; sku: string;
  volume: number; unit: string; quantity: number; unitPrice: Money; lineTotal: Money;
  availability: { status: string; availableQuantity: number }; issues: { code: string; message: string }[]; version: number;
};
export type Cart = {
  id: string | null; state: string; items: CartItem[]; subtotal: Money; canCheckout: boolean;
  issues: { code: string; message: string }[]; version: number; expiresAt: string | null;
};
export type Address = {
  id: string; recipientName: string; label?: string; postalCode: string; street: string; number: string;
  complement?: string; neighborhood: string; city: string; state: string; reference?: string; version: number;
};
export type Quote = {
  quoteId: string; serviceable: boolean; method: string; amount: string; currency: string;
  estimatedDelivery: { minDays: number; maxDays: number; dayType: string }; ruleVersion: string; expiresAt: string;
};
export type Order = {
  publicNumber: string; createdAt: string; updatedAt: string; status: string; paymentStatus: string;
  deliveryStatus: string; subtotal: Money; shipping: Money; total: Money; paymentExpiresAt?: string;
  items: { orderItemId: string; presentationId: string; productId: string; sku: string; productName: string;
    volume: number; unit: string; quantity: number; unitPrice: Money; lineTotal: Money }[];
  address: Record<string, string>; shippingSnapshot: { method: string; amount: Money; minDays: number; maxDays: number; dayType: string };
  history: { fromStatus: string; toStatus: string; event: string; createdAt: string }[];
};
export type OrderSummary = {
  publicNumber: string; createdAt: string; status: string; paymentStatus: string; deliveryStatus: string; total: Money; itemCount: number;
};
export type Payment = {
  paymentId: string; provider: string; providerPaymentId: string; status: string; amount: Money;
  checkoutUrl: string; createdAt: string; updatedAt: string; expiresAt: string;
};
export type AdminOrder = {
  order: Order; customer: { id: string; displayName: string; email: string };
  logistics: { carrier?: string; trackingCode?: string; shippedAt?: string; deliveredAt?: string };
  administrationHistory: { operation: string; reason?: string; createdAt: string }[];
  reservations: { reservationKey: string; presentationId: string; quantity: number; state: string }[];
  refunds: Refund[]; returns: ReturnRecord[];
};
export type Refund = { id: string; orderId: string; amount: Money; status: string; reason: string; providerRef?: string };
export type ReturnRecord = {
  id: string; orderItemId: string; presentationId: string; quantity: number; receivedQuantity: number;
  approvedQuantity: number; rejectedQuantity: number; status: string; reason: string; restockedQuantity?: number;
};
export type Problem = {
  type?: string; title?: string; status?: number; code?: string; message?: string; correlationId?: string;
  fieldErrors?: { field: string; code: string; message: string }[];
};
