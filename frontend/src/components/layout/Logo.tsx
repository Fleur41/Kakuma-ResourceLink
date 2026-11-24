import { Link } from 'react-router'

/** `inverse` is the white variant for the green sidebar, which is narrower, so its wordmark is a size smaller. */
export function Logo({ inverse }: { inverse?: boolean }) {
  return (
    <Link to="/" className={`flex min-w-0 items-center gap-2.5 rounded-md font-display font-bold tracking-tight ${inverse ? 'text-[17px]' : 'text-xl'}`}>
      <img src="/favicon.svg" alt="" width={32} height={32} className="shrink-0" />
      <span className={inverse ? 'text-white' : 'text-primary'}>
        Kakuma<span className={inverse ? 'text-white/85' : 'text-text'}>ResourceLink</span>
      </span>
    </Link>
  )
}
