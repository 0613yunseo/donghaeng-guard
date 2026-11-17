/** 서버가 준 imageUrl을 브라우저 주소로 바꾼다. 상대 경로(/images/...)는 개발 프록시·Vercel rewrite가 서버로 전달한다. */
export function imageSrc(url: string): string {
  return url.startsWith("http") ? url : `/${url.replace(/^\/+/, "")}`;
}
