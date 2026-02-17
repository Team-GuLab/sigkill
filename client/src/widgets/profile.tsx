import { Avatar, AvatarFallback, AvatarImage } from "@/ui/avatar";
import { useUser } from "@/store/user-store";

export default function Profile() {
  const user = useUser();
  const userInitial = user?.nickname
    ? user.nickname.charAt(0).toUpperCase()
    : "?";

  return (
    <div className="bg-background fixed right-0 bottom-0 left-0 flex items-center gap-3 border-t p-4">
      <Avatar>
        <AvatarImage src="" alt={user?.nickname || "User"} />
        <AvatarFallback>{userInitial}</AvatarFallback>
      </Avatar>
      <div className="flex flex-col">
        <span className="text-sm font-semibold">
          {user?.nickname || "Guest"}
        </span>
        <span className="text-muted-foreground text-xs">
          User ID: {user?.userId || "Unknown"}
        </span>
      </div>
    </div>
  );
}
