/* eslint-disable @next/next/no-img-element */
import { apiUrl } from "@/lib/api";
export function ProductImage({ src, alt, className = "" }: { src?: string; alt: string; className?: string }) {
  return src ? <img className={"product-image " + className} src={apiUrl(src)} alt={alt} loading="lazy" /> :
    <div className={"product-image placeholder " + className} aria-label={alt}>✦</div>;
}
