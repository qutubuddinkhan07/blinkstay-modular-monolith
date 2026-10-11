import ZoomImage from "../../../components/common/ZoomImage";

const UserAvatar = ({ user }) => (
  <ZoomImage
    src={user.profileImgUrl}
    alt={`${user.username}'s photo`}
    className="h-11 w-11 rounded-full"
    fallback={
      <span
        className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-text font-display font-semibold text-surface"
        aria-hidden="true"
      >
        {user.username?.[0]?.toUpperCase() ?? "U"}
      </span>
    }
  />
);

export default UserAvatar;
