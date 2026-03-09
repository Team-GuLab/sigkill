"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
} from "@/ui/dialog";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Field, FieldLabel } from "@/ui/field";
import { useCreateRoom } from "@/hooks/room/use-create-room";
import { useState } from "react";
import { toast } from "sonner";
import { ROUTE_GENERATORS } from "@/routes/paths";
import { useNavigate } from "react-router";
import { useSetRoomInfo } from "@/store/room-store";
import { X } from "lucide-react";

interface RoomCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RoomCreateModal({ open, onOpenChange }: RoomCreateModalProps) {
  const [title, setTitle] = useState("");
  const navigate = useNavigate();
  const setRoomInfo = useSetRoomInfo();

  const { mutate: createRoom, isPending: isCreatingRoom } = useCreateRoom({
    onSuccess: ({ room }) => {
      toast.success("방이 성공적으로 생성되었습니다!");
      setRoomInfo(room);
      onOpenChange(false);
      navigate(ROUTE_GENERATORS.WAITING_ROOM(room.roomId), { replace: true });
    },
    onError: error => {
      toast.error(error.message);
    },
  });

  const handleCreateRoomButtonClick = () => {
    createRoom({ roomTitle: title });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent showCloseButton={false}>
        <DialogHeader className="flex flex-row items-center justify-between">
          <DialogTitle>방 생성</DialogTitle>
          <DialogClose asChild>
            <button className="rounded-sm opacity-70 transition-opacity hover:opacity-100">
              <X className="h-4 w-4 text-black" />
            </button>
          </DialogClose>
        </DialogHeader>

        <Field>
          <FieldLabel htmlFor="room-title">방 제목</FieldLabel>
          <Input
            id="room-title"
            placeholder="방 제목을 입력하세요"
            required
            onChange={e => setTitle(e.target.value)}
            autoComplete="off"
          />
        </Field>

        <DialogFooter className="flex w-full flex-col gap-2 sm:flex-row sm:justify-stretch">
          <Button
            className="flex-1"
            disabled={!title || isCreatingRoom}
            onClick={handleCreateRoomButtonClick}
          >
            생성하기
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
