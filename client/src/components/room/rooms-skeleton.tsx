import { ItemGroup } from "@/ui/item";

function RoomItemSkeleton() {
  return (
    <div className="bg-card flex items-center gap-4 rounded-lg p-4">
      <div className="flex flex-1 items-center gap-2">
        <div className="bg-muted h-4 w-16 animate-pulse rounded" />
        <div className="bg-muted h-4 w-32 animate-pulse rounded" />
      </div>
      <div className="flex items-center gap-2">
        <div className="bg-muted h-4 w-10 animate-pulse rounded" />
        <div className="bg-muted h-5 w-14 animate-pulse rounded-full" />
      </div>
    </div>
  );
}

export function RoomsSkeleton({ count = 6 }: { count?: number }) {
  return (
    <ItemGroup className="gap-3">
      {Array.from({ length: count }).map((_, i) => (
        <RoomItemSkeleton key={i} />
      ))}
    </ItemGroup>
  );
}
