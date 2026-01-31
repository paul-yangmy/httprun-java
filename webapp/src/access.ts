/**
 * @see https://umijs.org/docs/max/access#access
 * */
export default function access(
  initialState: { currentUser?: HTTPRUN.CurrentUser } | undefined,
) {
  const { currentUser } = initialState ?? {};
  return {
    canAdmin: currentUser && currentUser.isAdmin,
  };
}
